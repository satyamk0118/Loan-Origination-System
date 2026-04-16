package com.turno.los.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * Request body for PUT /api/v1/agents/{agent_id}/loans/{loan_id}/decision
 */
@Data
public class AgentDecisionRequest {

    @NotNull(message = "Decision is required")
    @Pattern(regexp = "APPROVE|REJECT", message = "Decision must be APPROVE or REJECT")
    private String decision;
}
