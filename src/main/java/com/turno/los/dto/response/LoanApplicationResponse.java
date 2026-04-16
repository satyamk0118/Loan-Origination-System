package com.turno.los.dto.response;

import com.turno.los.enums.ApplicationStatus;
import com.turno.los.enums.LoanType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response body for loan application endpoints.
 */
@Data
@Builder
public class LoanApplicationResponse {
    private Long loanId;
    private String customerName;
    private String customerPhone;
    private BigDecimal loanAmount;
    private LoanType loanType;
    private ApplicationStatus applicationStatus;
    private Long assignedAgentId;
    private String assignedAgentName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
