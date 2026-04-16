package com.turno.los.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response body for GET /api/v1/customers/top
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopCustomerResponse {
    private String customerPhone;
    private String customerName;
    private long approvedLoanCount;
}
