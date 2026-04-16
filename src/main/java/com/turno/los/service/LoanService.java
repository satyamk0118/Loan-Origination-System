package com.turno.los.service;

import com.turno.los.dto.request.LoanApplicationRequest;
import com.turno.los.dto.response.LoanApplicationResponse;
import com.turno.los.dto.response.StatusCountResponse;
import com.turno.los.dto.response.TopCustomerResponse;
import com.turno.los.enums.ApplicationStatus;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Core loan-lifecycle operations exposed to the REST layer.
 */
public interface LoanService {

    /**
     * Submit a new loan application (status = APPLIED).
     */
    LoanApplicationResponse submitLoan(LoanApplicationRequest request);

    /**
     * Paginated fetch filtered by status.
     */
    Page<LoanApplicationResponse> getLoansByStatus(ApplicationStatus status, int page, int size);

    /**
     * Real-time counts of loans grouped by every possible status.
     */
    StatusCountResponse getStatusCounts();

    /**
     * Top 3 customers by total approved loans (system + agent approvals).
     */
    List<TopCustomerResponse> getTopCustomers();
}
