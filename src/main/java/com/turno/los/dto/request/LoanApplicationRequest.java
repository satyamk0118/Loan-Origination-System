package com.turno.los.dto.request;

import com.turno.los.enums.LoanType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Request body for POST /api/v1/loans
 */
@Data
public class LoanApplicationRequest {

    @NotBlank(message = "Customer name is required")
    @Size(max = 150, message = "Customer name must not exceed 150 characters")
    private String customerName;

    @NotBlank(message = "Customer phone is required")
    @Pattern(regexp = "^[+]?[0-9]{7,15}$", message = "Invalid phone number format")
    private String customerPhone;

    @NotNull(message = "Loan amount is required")
    @DecimalMin(value = "1000.00", message = "Minimum loan amount is 1000")
    @DecimalMax(value = "50000000.00", message = "Maximum loan amount is 50,000,000")
    private BigDecimal loanAmount;

    @NotNull(message = "Loan type is required")
    private LoanType loanType;
}
