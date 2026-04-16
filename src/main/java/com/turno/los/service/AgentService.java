package com.turno.los.service;

import com.turno.los.dto.request.AgentDecisionRequest;
import com.turno.los.dto.response.LoanApplicationResponse;
import com.turno.los.entity.Agent;
import com.turno.los.entity.LoanApplication;

/**
 * Agent-facing operations: reviewing loans and receiving assignments.
 */
public interface AgentService {

    /**
     * Assigns a loan to the next available agent and fires notifications.
     * Called internally by the loan processing job.
     *
     * @param loan the loan in UNDER_REVIEW status
     */
    void assignLoanToAgent(LoanApplication loan);

    /**
     * Records an agent's APPROVE / REJECT decision on a loan.
     *
     * @param agentId  the acting agent's id
     * @param loanId   the loan being decided upon
     * @param request  the decision payload
     * @return updated loan response
     */
    LoanApplicationResponse recordDecision(Long agentId, Long loanId, AgentDecisionRequest request);
}
