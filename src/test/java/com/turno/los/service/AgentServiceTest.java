package com.turno.los.service;

import com.turno.los.dto.request.AgentDecisionRequest;
import com.turno.los.dto.response.LoanApplicationResponse;
import com.turno.los.entity.Agent;
import com.turno.los.entity.LoanApplication;
import com.turno.los.enums.ApplicationStatus;
import com.turno.los.enums.LoanType;
import com.turno.los.exception.BusinessException;
import com.turno.los.exception.ResourceNotFoundException;
import com.turno.los.notification.NotificationService;
import com.turno.los.repository.AgentRepository;
import com.turno.los.repository.LoanApplicationRepository;
import com.turno.los.service.impl.AgentServiceImpl;
import com.turno.los.service.impl.LoanServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentServiceTest {

    @Mock private AgentRepository agentRepo;
    @Mock private LoanApplicationRepository loanRepo;
    @Mock private NotificationService notificationService;
    @Mock private LoanServiceImpl loanServiceMapper;

    @InjectMocks
    private AgentServiceImpl agentService;

    private Agent agent;
    private Agent manager;
    private LoanApplication underReviewLoan;

    @BeforeEach
    void setUp() {
        manager = Agent.builder().id(1L).name("Alice").email("alice@los.com").available(true).build();
        agent   = Agent.builder().id(2L).name("Bob").email("bob@los.com").manager(manager).available(true).build();

        underReviewLoan = LoanApplication.builder()
                .loanId(10L)
                .customerName("Jane Smith")
                .customerPhone("+919876543210")
                .loanAmount(new BigDecimal("2000000"))
                .loanType(LoanType.HOME)
                .applicationStatus(ApplicationStatus.UNDER_REVIEW)
                .assignedAgent(agent)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // -----------------------------------------------------------------------
    // assignLoanToAgent
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("assignLoanToAgent → assigns agent, notifies agent and manager")
    void assignLoanToAgent_shouldAssignAndNotifyBoth() {
        when(agentRepo.findAvailableAgentWithLock()).thenReturn(Optional.of(agent));
        when(loanRepo.save(any())).thenReturn(underReviewLoan);

        agentService.assignLoanToAgent(underReviewLoan);

        verify(loanRepo).save(underReviewLoan);
        verify(notificationService).sendAgentAssignmentNotification(agent, underReviewLoan);
        verify(notificationService).sendManagerNotification(manager, agent, underReviewLoan);
    }

    @Test
    @DisplayName("assignLoanToAgent → no manager, only agent notification sent")
    void assignLoanToAgent_noManager_shouldOnlyNotifyAgent() {
        agent.setManager(null);
        when(agentRepo.findAvailableAgentWithLock()).thenReturn(Optional.of(agent));
        when(loanRepo.save(any())).thenReturn(underReviewLoan);

        agentService.assignLoanToAgent(underReviewLoan);

        verify(notificationService).sendAgentAssignmentNotification(agent, underReviewLoan);
        verify(notificationService, never()).sendManagerNotification(any(), any(), any());
    }

    @Test
    @DisplayName("assignLoanToAgent → no available agents throws BusinessException")
    void assignLoanToAgent_noAgents_shouldThrow() {
        when(agentRepo.findAvailableAgentWithLock()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> agentService.assignLoanToAgent(underReviewLoan))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("No available agents");
    }

    // -----------------------------------------------------------------------
    // recordDecision — APPROVE
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("recordDecision APPROVE → status becomes APPROVED_BY_AGENT, SMS sent")
    void recordDecision_approve_shouldUpdateStatusAndSendSms() {
        LoanApplication approved = LoanApplication.builder()
                .loanId(10L).customerName("Jane Smith").customerPhone("+919876543210")
                .loanAmount(new BigDecimal("2000000")).loanType(LoanType.HOME)
                .applicationStatus(ApplicationStatus.APPROVED_BY_AGENT)
                .assignedAgent(agent).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();

        when(agentRepo.findById(2L)).thenReturn(Optional.of(agent));
        when(loanRepo.findByLoanIdAndAssignedAgentId(10L, 2L)).thenReturn(Optional.of(underReviewLoan));
        when(loanRepo.save(any())).thenReturn(approved);
        when(loanServiceMapper.toResponse(any())).thenReturn(LoanApplicationResponse.builder()
                .loanId(10L).applicationStatus(ApplicationStatus.APPROVED_BY_AGENT).build());

        AgentDecisionRequest req = new AgentDecisionRequest();
        req.setDecision("APPROVE");

        LoanApplicationResponse resp = agentService.recordDecision(2L, 10L, req);

        assertThat(resp.getApplicationStatus()).isEqualTo(ApplicationStatus.APPROVED_BY_AGENT);
        verify(notificationService).sendApprovalSmsToCustomer(any());
    }

    // -----------------------------------------------------------------------
    // recordDecision — REJECT
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("recordDecision REJECT → status becomes REJECTED_BY_AGENT, no SMS")
    void recordDecision_reject_shouldUpdateStatusNoSms() {
        when(agentRepo.findById(2L)).thenReturn(Optional.of(agent));
        when(loanRepo.findByLoanIdAndAssignedAgentId(10L, 2L)).thenReturn(Optional.of(underReviewLoan));
        when(loanRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(loanServiceMapper.toResponse(any())).thenReturn(LoanApplicationResponse.builder()
                .loanId(10L).applicationStatus(ApplicationStatus.REJECTED_BY_AGENT).build());

        AgentDecisionRequest req = new AgentDecisionRequest();
        req.setDecision("REJECT");

        LoanApplicationResponse resp = agentService.recordDecision(2L, 10L, req);

        assertThat(resp.getApplicationStatus()).isEqualTo(ApplicationStatus.REJECTED_BY_AGENT);
        verify(notificationService, never()).sendApprovalSmsToCustomer(any());
    }

    // -----------------------------------------------------------------------
    // recordDecision — error cases
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("recordDecision → agent not found throws ResourceNotFoundException")
    void recordDecision_agentNotFound_shouldThrow() {
        when(agentRepo.findById(99L)).thenReturn(Optional.empty());

        AgentDecisionRequest req = new AgentDecisionRequest();
        req.setDecision("APPROVE");

        assertThatThrownBy(() -> agentService.recordDecision(99L, 10L, req))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Agent not found");
    }

    @Test
    @DisplayName("recordDecision → loan not assigned to agent throws ResourceNotFoundException")
    void recordDecision_loanNotAssigned_shouldThrow() {
        when(agentRepo.findById(2L)).thenReturn(Optional.of(agent));
        when(loanRepo.findByLoanIdAndAssignedAgentId(10L, 2L)).thenReturn(Optional.empty());

        AgentDecisionRequest req = new AgentDecisionRequest();
        req.setDecision("APPROVE");

        assertThatThrownBy(() -> agentService.recordDecision(2L, 10L, req))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("not assigned to agent");
    }

    @Test
    @DisplayName("recordDecision → loan not in UNDER_REVIEW throws BusinessException")
    void recordDecision_wrongStatus_shouldThrow() {
        underReviewLoan.setApplicationStatus(ApplicationStatus.APPROVED_BY_SYSTEM);
        when(agentRepo.findById(2L)).thenReturn(Optional.of(agent));
        when(loanRepo.findByLoanIdAndAssignedAgentId(10L, 2L)).thenReturn(Optional.of(underReviewLoan));

        AgentDecisionRequest req = new AgentDecisionRequest();
        req.setDecision("APPROVE");

        assertThatThrownBy(() -> agentService.recordDecision(2L, 10L, req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("cannot be decided upon");
    }
}
