package com.turno.los.controller;

import com.turno.los.dto.request.LoanApplicationRequest;
import com.turno.los.dto.response.ApiResponse;
import com.turno.los.dto.response.LoanApplicationResponse;
import com.turno.los.dto.response.StatusCountResponse;
import com.turno.los.dto.response.TopCustomerResponse;
import com.turno.los.enums.ApplicationStatus;
import com.turno.los.service.LoanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST endpoints for loan application lifecycle and reporting.
 *
 * POST   /api/v1/loans                  → submit a loan application
 * GET    /api/v1/loans                  → fetch loans by status (paginated)
 * GET    /api/v1/loans/status-count     → real-time status counts
 * GET    /api/v1/customers/top          → top 3 customers by approvals
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class LoanController {

    private final LoanService loanService;

    /**
     * Submit a new loan application.
     *
     * POST /api/v1/loans
     */
    @PostMapping("/loans")
    public ResponseEntity<ApiResponse<LoanApplicationResponse>> submitLoan(
            @Valid @RequestBody LoanApplicationRequest request) {

        LoanApplicationResponse response = loanService.submitLoan(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Loan application submitted successfully."));
    }

    /**
     * Fetch loans filtered by status with pagination.
     *
     * GET /api/v1/loans?status=APPLIED&page=0&size=10
     */
    @GetMapping("/loans")
    public ResponseEntity<ApiResponse<Page<LoanApplicationResponse>>> getLoansByStatus(
            @RequestParam ApplicationStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<LoanApplicationResponse> result = loanService.getLoansByStatus(status, page, size);
        return ResponseEntity.ok(ApiResponse.success(result,
                "Fetched " + result.getTotalElements() + " loan(s) with status " + status));
    }

    /**
     * Real-time loan counts grouped by status.
     *
     * GET /api/v1/loans/status-count
     */
    @GetMapping("/loans/status-count")
    public ResponseEntity<ApiResponse<StatusCountResponse>> getStatusCount() {
        return ResponseEntity.ok(ApiResponse.success(loanService.getStatusCounts(), "Status counts retrieved."));
    }

    /**
     * Top 3 customers with the most approved loans.
     *
     * GET /api/v1/customers/top
     */
    @GetMapping("/customers/top")
    public ResponseEntity<ApiResponse<List<TopCustomerResponse>>> getTopCustomers() {
        List<TopCustomerResponse> top = loanService.getTopCustomers();
        return ResponseEntity.ok(ApiResponse.success(top, "Top customers retrieved."));
    }
}
