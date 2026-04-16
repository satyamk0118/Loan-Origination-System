package com.turno.los.service;

import com.turno.los.entity.LoanApplication;
import com.turno.los.enums.ApplicationStatus;
import com.turno.los.enums.LoanType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class LoanProcessingJobTest {

    @Mock private com.turno.los.repository.LoanApplicationRepository loanRepo;
    @Mock private AgentService agentService;
    @Mock private com.turno.los.notification.NotificationService notificationService;
    @Mock private java.util.concurrent.ExecutorService executorService;

    @InjectMocks
    private LoanProcessingJob job;

    private LoanApplication lowValueLoan;
    private LoanApplication highValueLoan;

    @BeforeEach
    void setUp() {
        lowValueLoan = LoanApplication.builder()
                .loanId(1L)
                .customerName("Alice")
                .customerPhone("+911111111111")
                .loanAmount(new BigDecimal("500000"))   // Below 10L threshold
                .loanType(LoanType.PERSONAL)
                .applicationStatus(ApplicationStatus.APPLIED)
                .build();

        highValueLoan = LoanApplication.builder()
                .loanId(2L)
                .customerName("Bob")
                .customerPhone("+912222222222")
                .loanAmount(new BigDecimal("5000000"))  // Above 10L threshold
                .loanType(LoanType.HOME)
                .applicationStatus(ApplicationStatus.APPLIED)
                .build();
    }

    // -----------------------------------------------------------------------
    // decide() — high value
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("decide → amount > 10,00,000 always returns UNDER_REVIEW")
    void decide_highValue_shouldAlwaysReturnUnderReview() {
        // Run many times — should ALWAYS be UNDER_REVIEW regardless of random
        Set<ApplicationStatus> results = IntStream.range(0, 100)
                .mapToObj(i -> job.decide(highValueLoan))
                .collect(Collectors.toSet());

        assertThat(results).containsOnly(ApplicationStatus.UNDER_REVIEW);
    }

    // -----------------------------------------------------------------------
    // decide() — low value
    // -----------------------------------------------------------------------

    @RepeatedTest(50)
    @DisplayName("decide → low-value loan returns either APPROVED or REJECTED (never UNDER_REVIEW)")
    void decide_lowValue_shouldNeverReturnUnderReview() {
        ApplicationStatus result = job.decide(lowValueLoan);

        assertThat(result).isIn(
                ApplicationStatus.APPROVED_BY_SYSTEM,
                ApplicationStatus.REJECTED_BY_SYSTEM
        );
        assertThat(result).isNotEqualTo(ApplicationStatus.UNDER_REVIEW);
    }

    @Test
    @DisplayName("decide → low-value loan produces both outcomes across many runs (probabilistic)")
    void decide_lowValue_shouldProduceBothOutcomesEventually() {
        Set<ApplicationStatus> outcomes = IntStream.range(0, 1000)
                .mapToObj(i -> job.decide(lowValueLoan))
                .collect(Collectors.toSet());

        // With 20% rejection rate over 1000 runs, both outcomes should appear
        assertThat(outcomes).contains(
                ApplicationStatus.APPROVED_BY_SYSTEM,
                ApplicationStatus.REJECTED_BY_SYSTEM
        );
    }
}
