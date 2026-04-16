package com.turno.los.notification;

import com.turno.los.entity.Agent;
import com.turno.los.entity.LoanApplication;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Mock implementation of {@link NotificationService}.
 *
 * All methods log the notification event so they can be verified in tests
 * and observed in local runs without connecting to real push/SMS providers.
 *
 * Replace this class with a real implementation (e.g. FCM + Twilio) without
 * touching any caller — the interface is the only coupling point.
 */
@Service
@Slf4j
public class MockNotificationService implements NotificationService {

    @Override
    public void sendAgentAssignmentNotification(Agent agent, LoanApplication loan) {
        log.info(
            "[PUSH NOTIFICATION] → Agent '{}' (id={}) assigned to Loan #{} " +
            "(customer: {}, amount: {}, type: {})",
            agent.getName(),
            agent.getId(),
            loan.getLoanId(),
            loan.getCustomerName(),
            loan.getLoanAmount(),
            loan.getLoanType()
        );
    }

    @Override
    public void sendManagerNotification(Agent manager, Agent agent, LoanApplication loan) {
        log.info(
            "[PUSH NOTIFICATION] → Manager '{}' (id={}) notified: Agent '{}' assigned to Loan #{}",
            manager.getName(),
            manager.getId(),
            agent.getName(),
            loan.getLoanId()
        );
    }

    @Override
    public void sendApprovalSmsToCustomer(LoanApplication loan) {
        log.info(
            "[SMS] → Customer '{}' at '{}': Congratulations! Your loan #{} of {} ({}) has been APPROVED.",
            loan.getCustomerName(),
            loan.getCustomerPhone(),
            loan.getLoanId(),
            loan.getLoanAmount(),
            loan.getLoanType()
        );
    }
}
