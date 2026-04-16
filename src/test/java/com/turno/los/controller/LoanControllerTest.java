package com.turno.los.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.turno.los.dto.request.LoanApplicationRequest;
import com.turno.los.dto.response.LoanApplicationResponse;
import com.turno.los.dto.response.StatusCountResponse;
import com.turno.los.enums.ApplicationStatus;
import com.turno.los.enums.LoanType;
import com.turno.los.service.LoanService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LoanController.class)
class LoanControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean  private LoanService loanService;

    private LoanApplicationResponse sampleResponse() {
        return LoanApplicationResponse.builder()
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
    // POST /api/v1/loans
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("POST /api/v1/loans → 201 with valid body")
    void submitLoan_valid_returns201() throws Exception {
        LoanApplicationRequest req = new LoanApplicationRequest();
        req.setCustomerName("John Doe");
        req.setCustomerPhone("+911234567890");
        req.setLoanAmount(new BigDecimal("500000"));
        req.setLoanType(LoanType.PERSONAL);

        when(loanService.submitLoan(any())).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/v1/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.loanId").value(1))
                .andExpect(jsonPath("$.data.applicationStatus").value("APPLIED"));
    }

    @Test
    @DisplayName("POST /api/v1/loans → 400 when customerName is blank")
    void submitLoan_blankName_returns400() throws Exception {
        LoanApplicationRequest req = new LoanApplicationRequest();
        req.setCustomerName("");
        req.setCustomerPhone("+911234567890");
        req.setLoanAmount(new BigDecimal("500000"));
        req.setLoanType(LoanType.PERSONAL);

        mockMvc.perform(post("/api/v1/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("POST /api/v1/loans → 400 when loanAmount is below minimum")
    void submitLoan_amountTooLow_returns400() throws Exception {
        LoanApplicationRequest req = new LoanApplicationRequest();
        req.setCustomerName("John Doe");
        req.setCustomerPhone("+911234567890");
        req.setLoanAmount(new BigDecimal("1"));   // below 1000
        req.setLoanType(LoanType.PERSONAL);

        mockMvc.perform(post("/api/v1/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // -----------------------------------------------------------------------
    // GET /api/v1/loans?status=APPLIED
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("GET /api/v1/loans?status=APPLIED → 200 with paginated results")
    void getLoansByStatus_returns200() throws Exception {
        when(loanService.getLoansByStatus(eq(ApplicationStatus.APPLIED), eq(0), eq(10)))
                .thenReturn(new PageImpl<>(List.of(sampleResponse())));

        mockMvc.perform(get("/api/v1/loans")
                        .param("status", "APPLIED")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].loanId").value(1));
    }

    // -----------------------------------------------------------------------
    // GET /api/v1/loans/status-count
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("GET /api/v1/loans/status-count → 200 with status map")
    void getStatusCount_returns200() throws Exception {
        Map<ApplicationStatus, Long> counts = new EnumMap<>(ApplicationStatus.class);
        counts.put(ApplicationStatus.APPLIED, 5L);
        counts.put(ApplicationStatus.APPROVED_BY_SYSTEM, 3L);

        when(loanService.getStatusCounts()).thenReturn(new StatusCountResponse(counts, 8L));

        mockMvc.perform(get("/api/v1/loans/status-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(8))
                .andExpect(jsonPath("$.data.counts.APPLIED").value(5));
    }

    // -----------------------------------------------------------------------
    // GET /api/v1/customers/top
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("GET /api/v1/customers/top → 200 with top customer list")
    void getTopCustomers_returns200() throws Exception {
        when(loanService.getTopCustomers()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/customers/top"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
