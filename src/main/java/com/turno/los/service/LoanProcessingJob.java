package com.turno.los.service;

import com.turno.los.entity.LoanApplication;
import com.turno.los.enums.ApplicationStatus;
import com.turno.los.notification.NotificationService;
import com.turno.los.repository.LoanApplicationRepository;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ExecutorService;

/**
 * Scheduled background job that simulates automated loan processing.
 *
 * <h3>Thread-Safety Strategy</h3>
 * <ul>
 *   <li>The scheduler fires on a fixed interval and submits a task to the
 *       thread pool for each APPLIED loan it can find.</li>
 *   <li>{@code findNextAppliedLoanWithLock()} uses {@code FOR UPDATE SKIP LOCKED}
 *       so concurrent threads never pick the same row.</li>
 *   <li>Status transitions are wrapped in {@code @Transactional} to ensure
 *       atomicity — partial updates are rolled back on failure.</li>
 * </ul>
 *
 * <h3>Decision Rules</h3>
 * <ul>
 *   <li>Loan amount &gt; 1,000,000  → UNDER_REVIEW (high-value needs human check)</li>
 *   <li>Random 20% of remaining    → REJECTED_BY_SYSTEM (credit simulation)</li>
 *   <li>Remaining 80%              → APPROVED_BY_SYSTEM</li>
 * </ul>
 */
@Service
@Slf4j
public class LoanProcessingJob {

    private static final BigDecimal HIGH_VALUE_THRESHOLD = new BigDecimal("1000000");

    private final LoanApplicationRepository loanRepo;
    private final AgentService agentService;
    private final NotificationService notificationService;
    private final ExecutorService executor;
    private final Random random = new Random();

    @Value("${loan.processing.delay-min-seconds:15}")
    private int delayMinSeconds;

    @Value("${loan.processing.delay-max-seconds:25}")
    private int delayMaxSeconds;

    public LoanProcessingJob(
            LoanApplicationRepository loanRepo,
            AgentService agentService,
            NotificationService notificationService,
            @Qualifier("loanProcessingExecutor") ExecutorService executor) {
        this.loanRepo = loanRepo;
        this.agentService = agentService;
        this.notificationService = notificationService;
        this.executor = executor;
    }

    /**
     * Runs every {@code loan.processing.scheduler-interval-ms} milliseconds.
     *
     * Drains all currently APPLIED loans into the thread pool; each is
     * processed independently with its own simulated delay.
     */
    @Scheduled(fixedDelayString = "${loan.processing.scheduler-interval-ms:10000}")
    public void scheduleProcessing() {
        log.debug("LoanProcessingJob tick — scanning for APPLIED loans...");

        // Keep picking loans until none are left for this tick
        int submitted = 0;
        while (true) {
            Optional<LoanApplication> next = fetchNextLoan();
            if (next.isEmpty()) break;

            LoanApplication loan = next.get();
            executor.submit(() -> processLoan(loan.getLoanId()));
            submitted++;
        }

        if (submitted > 0) {
            log.info("LoanProcessingJob: submitted {} loans for processing.", submitted);
        }
    }

    /**
     * Fetches the next APPLIED loan using a database-level row lock.
     * Wrapped in its own transaction so the lock is released promptly.
     */
    @Transactional
    public Optional<LoanApplication> fetchNextLoan() {
        return loanRepo.findNextAppliedLoanWithLock();
    }

    /**
     * Executed by a thread-pool worker:
     * 1. Simulates a system check delay.
     * 2. Applies business rules to decide the outcome.
     * 3. Persists the new status.
     * 4. Triggers agent assignment if UNDER_REVIEW.
     * 5. Sends SMS if APPROVED_BY_SYSTEM.
     */
    private void processLoan(Long loanId) {
        log.info("[Thread {}] Starting processing for loan #{}", Thread.currentThread().getName(), loanId);

        try {
            simulateDelay();
            applyDecision(loanId);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("[Thread {}] Interrupted while processing loan #{}", Thread.currentThread().getName(), loanId);
        } catch (Exception e) {
            log.error("[Thread {}] Error processing loan #{}: {}", Thread.currentThread().getName(), loanId, e.getMessage(), e);
        }
    }

    /**
     * Sleeps for a random duration between delayMinSeconds and delayMaxSeconds
     * to simulate external system checks (credit bureau, fraud detection, etc.).
     */
    private void simulateDelay() throws InterruptedException {
        int range = delayMaxSeconds - delayMinSeconds;
        int delaySec = delayMinSeconds + (range > 0 ? random.nextInt(range) : 0);
        log.debug("[Thread {}] Simulating {}s system check delay...", Thread.currentThread().getName(), delaySec);
        Thread.sleep(delaySec * 1000L);
    }

    /**
     * Applies decision rules within a dedicated transaction.
     * Re-fetches the loan to ensure we work on the latest DB state.
     */
    @Transactional
    public void applyDecision(Long loanId) {
        LoanApplication loan = loanRepo.findById(loanId).orElse(null);
        if (loan == null) {
            log.warn("Loan #{} not found during decision — skipping.", loanId);
            return;
        }

        // Guard: another thread may have changed status between fetch and decision
        if (loan.getApplicationStatus() != ApplicationStatus.APPLIED) {
            log.warn("Loan #{} is no longer APPLIED (status={}). Skipping.", loanId, loan.getApplicationStatus());
            return;
        }

        ApplicationStatus decision = decide(loan);
        loan.setApplicationStatus(decision);
        loanRepo.save(loan);

        log.info("[Thread {}] Loan #{} → {}", Thread.currentThread().getName(), loanId, decision);

        // Post-decision side effects
        if (decision == ApplicationStatus.UNDER_REVIEW) {
            agentService.assignLoanToAgent(loan);
        } else if (decision == ApplicationStatus.APPROVED_BY_SYSTEM) {
            notificationService.sendApprovalSmsToCustomer(loan);
        }
    }

    /**
     * Pure decision function — no DB access, easily unit-testable.
     *
     * Rules (evaluated in order):
     *  1. Amount > 10,00,000 → UNDER_REVIEW
     *  2. Random 20%         → REJECTED_BY_SYSTEM
     *  3. Otherwise          → APPROVED_BY_SYSTEM
     */
    ApplicationStatus decide(LoanApplication loan) {
        if (loan.getLoanAmount().compareTo(HIGH_VALUE_THRESHOLD) > 0) {
            return ApplicationStatus.UNDER_REVIEW;
        }
        if (random.nextDouble() < 0.20) {
            return ApplicationStatus.REJECTED_BY_SYSTEM;
        }
        return ApplicationStatus.APPROVED_BY_SYSTEM;
    }

    /** Graceful shutdown — waits for in-flight tasks to complete. */
    @PreDestroy
    public void shutdown() {
        log.info("LoanProcessingJob shutting down executor...");
        executor.shutdown();
    }
}
