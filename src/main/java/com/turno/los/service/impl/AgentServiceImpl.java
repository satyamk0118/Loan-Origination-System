package com.turno.los.service.impl;

import com.turno.los.dto.request.AgentDecisionRequest;
import com.turno.los.dto.response.LoanApplicationResponse;
import com.turno.los.entity.Agent;
import com.turno.los.entity.LoanApplication;
import com.turno.los.enums.ApplicationStatus;
import com.turno.los.exception.BusinessException;
import com.turno.los.exception.ResourceNotFoundException;
import com.turno.los.notification.NotificationService;
import com.turno.los.repository.AgentRepository;
import com.turno.los.repository.LoanApplicationRepository;
import com.turno.los.service.AgentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgentServiceImpl implements AgentService {

    private final AgentRepository agentRepo;
    private final LoanApplicationRepository loanRepo;
    private final NotificationService notificationService;
    private final LoanServiceImpl loanServiceMapper;   // reuse the toResponse mapper

    // -----------------------------------------------------------------------
    // Assign loan to an available agent (called by background job)
    // -----------------------------------------------------------------------

    @Override
    @Transactional
    public void assignLoanToAgent(LoanApplication loan) {
        // FOR UPDATE SKIP LOCKED ensures only one thread claims this agent row
        Agent agent = agentRepo.findAvailableAgentWithLock()
                .orElseThrow(() -> new BusinessException(
                        "No available agents at the moment. Loan " + loan.getLoanId() + " remains UNDER_REVIEW."));

        loan.setAssignedAgent(agent);
        loanRepo.save(loan);

        log.info("Loan #{} assigned to agent '{}' (id={})", loan.getLoanId(), agent.getName(), agent.getId());

        // Notify the assigned agent
        notificationService.sendAgentAssignmentNotification(agent, loan);

        // Notify the agent's manager (if any)
        if (agent.getManager() != null) {
            notificationService.sendManagerNotification(agent.getManager(), agent, loan);
        }
    }

    // -----------------------------------------------------------------------
    // Record agent's decision
    // -----------------------------------------------------------------------

    @Override
    @Transactional
    public LoanApplicationResponse recordDecision(Long agentId, Long loanId, AgentDecisionRequest request) {
        // Verify the agent exists
        agentRepo.findById(agentId)
                .orElseThrow(() -> new ResourceNotFoundException("Agent not found with id: " + agentId));

        // Verify the loan exists AND belongs to this agent
        LoanApplication loan = loanRepo.findByLoanIdAndAssignedAgentId(loanId, agentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Loan #" + loanId + " is not assigned to agent " + agentId));

        // Only loans in UNDER_REVIEW can be decided
        if (loan.getApplicationStatus() != ApplicationStatus.UNDER_REVIEW) {
            throw new BusinessException(
                    "Loan #" + loanId + " is in status '" + loan.getApplicationStatus() +
                    "' and cannot be decided upon. Only UNDER_REVIEW loans are actionable.");
        }

        boolean approved = "APPROVE".equalsIgnoreCase(request.getDecision());
        ApplicationStatus newStatus = approved
                ? ApplicationStatus.APPROVED_BY_AGENT
                : ApplicationStatus.REJECTED_BY_AGENT;

        loan.setApplicationStatus(newStatus);
        LoanApplication updated = loanRepo.save(loan);

        log.info("Agent {} set loan #{} → {}", agentId, loanId, newStatus);

        // Send SMS to customer on approval
        if (approved) {
            notificationService.sendApprovalSmsToCustomer(updated);
        }

        return loanServiceMapper.toResponse(updated);
    }
}
