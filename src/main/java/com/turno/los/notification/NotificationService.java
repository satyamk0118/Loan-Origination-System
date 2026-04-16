package com.turno.los.notification;

import com.turno.los.entity.Agent;
import com.turno.los.entity.LoanApplication;

/**
 * Contract for the notification service.
 *
 * Decoupled from any transport mechanism — real implementations could
 * use FCM, Twilio, SNS etc. without changing the callers.
 */
public interface NotificationService {

    /**
     * Push notification sent to an agent when a loan is assigned to them.
     *
     * @param agent the assigned agent
     * @param loan  the loan application
     */
    void sendAgentAssignmentNotification(Agent agent, LoanApplication loan);

    /**
     * Push notification sent to the agent's manager when a loan is assigned.
     *
     * @param manager the agent's manager
     * @param agent   the agent who received the assignment
     * @param loan    the loan application
     */
    void sendManagerNotification(Agent manager, Agent agent, LoanApplication loan);

    /**
     * SMS notification sent to the customer when their loan is approved.
     *
     * @param loan the approved loan application
     */
    void sendApprovalSmsToCustomer(LoanApplication loan);
}
