package com.turno.los.service.impl;

import com.turno.los.dto.request.LoanApplicationRequest;
import com.turno.los.dto.response.LoanApplicationResponse;
import com.turno.los.dto.response.StatusCountResponse;
import com.turno.los.dto.response.TopCustomerResponse;
import com.turno.los.entity.LoanApplication;
import com.turno.los.enums.ApplicationStatus;
import com.turno.los.repository.LoanApplicationRepository;
import com.turno.los.service.LoanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoanServiceImpl implements LoanService {

    private final LoanApplicationRepository loanRepo;

    // -----------------------------------------------------------------------
    // Submit
    // -----------------------------------------------------------------------

    @Override
    @Transactional
    public LoanApplicationResponse submitLoan(LoanApplicationRequest request) {
        LoanApplication loan = LoanApplication.builder()
                .customerName(request.getCustomerName())
                .customerPhone(request.getCustomerPhone())
                .loanAmount(request.getLoanAmount())
                .loanType(request.getLoanType())
                .applicationStatus(ApplicationStatus.APPLIED)
                .build();

        LoanApplication saved = loanRepo.save(loan);
        log.info("New loan application submitted: id={}, customer={}, amount={}",
                saved.getLoanId(), saved.getCustomerName(), saved.getLoanAmount());

        return toResponse(saved);
    }

    // -----------------------------------------------------------------------
    // Paginated fetch by status
    // -----------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public Page<LoanApplicationResponse> getLoansByStatus(ApplicationStatus status, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return loanRepo.findByApplicationStatus(status, pageable).map(this::toResponse);
    }

    // -----------------------------------------------------------------------
    // Status counts
    // -----------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public StatusCountResponse getStatusCounts() {
        // Initialise all statuses to 0 so missing ones still appear in the response
        Map<ApplicationStatus, Long> counts = new EnumMap<>(ApplicationStatus.class);
        Arrays.stream(ApplicationStatus.values()).forEach(s -> counts.put(s, 0L));

        loanRepo.countByStatus().forEach(row -> counts.put((ApplicationStatus) row[0], (Long) row[1]));

        long total = counts.values().stream().mapToLong(Long::longValue).sum();
        return new StatusCountResponse(counts, total);
    }

    // -----------------------------------------------------------------------
    // Top customers
    // -----------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public List<TopCustomerResponse> getTopCustomers() {
        PageRequest top3 = PageRequest.of(0, 3);
        return loanRepo.findTopCustomers(top3).stream()
                .map(row -> new TopCustomerResponse(
                        (String) row[0],
                        (String) row[1],
                        (Long) row[2]))
                .collect(Collectors.toList());
    }

    // -----------------------------------------------------------------------
    // Mapper
    // -----------------------------------------------------------------------

    public LoanApplicationResponse toResponse(LoanApplication loan) {
        return LoanApplicationResponse.builder()
                .loanId(loan.getLoanId())
                .customerName(loan.getCustomerName())
                .customerPhone(loan.getCustomerPhone())
                .loanAmount(loan.getLoanAmount())
                .loanType(loan.getLoanType())
                .applicationStatus(loan.getApplicationStatus())
                .assignedAgentId(loan.getAssignedAgent() != null ? loan.getAssignedAgent().getId() : null)
                .assignedAgentName(loan.getAssignedAgent() != null ? loan.getAssignedAgent().getName() : null)
                .createdAt(loan.getCreatedAt())
                .updatedAt(loan.getUpdatedAt())
                .build();
    }
}
