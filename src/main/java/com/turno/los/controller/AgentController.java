package com.turno.los.controller;

import com.turno.los.dto.request.AgentDecisionRequest;
import com.turno.los.dto.response.ApiResponse;
import com.turno.los.dto.response.LoanApplicationResponse;
import com.turno.los.service.AgentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST endpoint for agent loan decisions.
 *
 * PUT /api/v1/agents/{agent_id}/loans/{loan_id}/decision
 */
@RestController
@RequestMapping("/api/v1/agents")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;

    /**
     * Agent approves or rejects a loan under review.
     *
     * PUT /api/v1/agents/{agent_id}/loans/{loan_id}/decision
     * Body: { "decision": "APPROVE" | "REJECT" }
     */
    @PutMapping("/{agentId}/loans/{loanId}/decision")
    public ResponseEntity<ApiResponse<LoanApplicationResponse>> recordDecision(
            @PathVariable Long agentId,
            @PathVariable Long loanId,
            @Valid @RequestBody AgentDecisionRequest request) {

        LoanApplicationResponse response = agentService.recordDecision(agentId, loanId, request);
        String msg = "APPROVE".equalsIgnoreCase(request.getDecision())
                ? "Loan #" + loanId + " approved by agent " + agentId
                : "Loan #" + loanId + " rejected by agent " + agentId;

        return ResponseEntity.ok(ApiResponse.success(response, msg));
    }
}
