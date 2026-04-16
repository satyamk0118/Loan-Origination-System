package com.turno.los.service;

import com.turno.los.dto.request.LoanApplicationRequest;
import com.turno.los.dto.response.LoanApplicationResponse;
import com.turno.los.dto.response.StatusCountResponse;
import com.turno.los.dto.response.TopCustomerResponse;
import com.turno.los.entity.LoanApplication;
import com.turno.los.enums.ApplicationStatus;
import com.turno.los.enums.LoanType;
import com.turno.los.repository.LoanApplicationRepository;
import com.turno.los.service.impl.LoanServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    @Mock
    private LoanApplicationRepository loanRepo;

    @InjectMocks
    private LoanServiceImpl loanService;

    private LoanApplication sampleLoan;

    @BeforeEach
    void setUp() {
        sampleLoan = LoanApplication.builder()
                .loanId(1L)
                .customerName("John Doe")
                .customerPhone("+911234567890")
                .loanAmount(new BigDecimal("500000"))
                .loanType(LoanType.PERSONAL)
                .applicationStatus(ApplicationStatus.APPLIED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // -----------------------------------------------------------------------
    // submitLoan
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("submitLoan → persists loan with APPLIED status and returns response")
    void submitLoan_shouldPersistAndReturnResponse() {
        when(loanRepo.save(any(LoanApplication.class))).thenReturn(sampleLoan);

        LoanApplicationRequest req = new LoanApplicationRequest();
        req.setCustomerName("John Doe");
        req.setCustomerPhone("+911234567890");
        req.setLoanAmount(new BigDecimal("500000"));
        req.setLoanType(LoanType.PERSONAL);

        LoanApplicationResponse response = loanService.submitLoan(req);

        assertThat(response.getLoanId()).isEqualTo(1L);
        assertThat(response.getApplicationStatus()).isEqualTo(ApplicationStatus.APPLIED);
        assertThat(response.getCustomerName()).isEqualTo("John Doe");
        verify(loanRepo, times(1)).save(any(LoanApplication.class));
    }

    // -----------------------------------------------------------------------
    // getLoansByStatus
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("getLoansByStatus → returns paged results for given status")
    void getLoansByStatus_shouldReturnPagedResults() {
        Page<LoanApplication> page = new PageImpl<>(List.of(sampleLoan));
        when(loanRepo.findByApplicationStatus(eq(ApplicationStatus.APPLIED), any(Pageable.class)))
                .thenReturn(page);

        Page<LoanApplicationResponse> result = loanService.getLoansByStatus(ApplicationStatus.APPLIED, 0, 10);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getApplicationStatus()).isEqualTo(ApplicationStatus.APPLIED);
    }

    // -----------------------------------------------------------------------
    // getStatusCounts
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("getStatusCounts → returns counts for all statuses, zeroes for missing ones")
    void getStatusCounts_shouldReturnCountsWithZeroDefaults() {
        List<Object[]> rawCounts = List.of(
                new Object[]{ApplicationStatus.APPLIED, 5L},
                new Object[]{ApplicationStatus.APPROVED_BY_SYSTEM, 3L}
        );
        when(loanRepo.countByStatus()).thenReturn(rawCounts);

        StatusCountResponse resp = loanService.getStatusCounts();

        assertThat(resp.getCounts().get(ApplicationStatus.APPLIED)).isEqualTo(5L);
        assertThat(resp.getCounts().get(ApplicationStatus.APPROVED_BY_SYSTEM)).isEqualTo(3L);
        // Statuses not in query result should default to 0
        assertThat(resp.getCounts().get(ApplicationStatus.REJECTED_BY_SYSTEM)).isEqualTo(0L);
        assertThat(resp.getTotal()).isEqualTo(8L);
    }

    // -----------------------------------------------------------------------
    // getTopCustomers
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("getTopCustomers → maps raw query results to TopCustomerResponse list")
    void getTopCustomers_shouldReturnMappedList() {
        List<Object[]> rawRows = List.of(
                new Object[]{"+911111111111", "Alice", 10L},
                new Object[]{"+912222222222", "Bob", 7L},
                new Object[]{"+913333333333", "Carol", 5L}
        );
        when(loanRepo.findTopCustomers(any(Pageable.class))).thenReturn(rawRows);

        List<TopCustomerResponse> result = loanService.getTopCustomers();

        assertThat(result).hasSize(3);
        assertThat(result.get(0).getCustomerName()).isEqualTo("Alice");
        assertThat(result.get(0).getApprovedLoanCount()).isEqualTo(10L);
        assertThat(result.get(2).getCustomerName()).isEqualTo("Carol");
    }
}
