package com.turno.los.repository;

import com.turno.los.entity.LoanApplication;
import com.turno.los.enums.ApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LoanApplicationRepository extends JpaRepository<LoanApplication, Long> {

    /**
     * Paginated fetch by status — used by GET /api/v1/loans?status=&page=&size=
     */
    Page<LoanApplication> findByApplicationStatus(ApplicationStatus status, Pageable pageable);

    /**
     * Count of loans grouped by status — used by GET /api/v1/loans/status-count
     */
    @Query("SELECT l.applicationStatus, COUNT(l) FROM LoanApplication l GROUP BY l.applicationStatus")
    List<Object[]> countByStatus();

    /**
     * Top customers by approved loan count.
     * Considers both APPROVED_BY_SYSTEM and APPROVED_BY_AGENT.
     */
    @Query("""
        SELECT l.customerPhone, l.customerName, COUNT(l)
        FROM LoanApplication l
        WHERE l.applicationStatus IN (
            com.turno.los.enums.ApplicationStatus.APPROVED_BY_SYSTEM,
            com.turno.los.enums.ApplicationStatus.APPROVED_BY_AGENT
        )
        GROUP BY l.customerPhone, l.customerName
        ORDER BY COUNT(l) DESC
        """)
    List<Object[]> findTopCustomers(Pageable pageable);

    /**
     * Used by background processor to fetch the next unprocessed loan with
     * a pessimistic write lock to prevent concurrent threads picking the same row.
     */
    @Query(value = """
        SELECT * FROM loan_applications
        WHERE application_status = 'APPLIED'
        ORDER BY created_at ASC
        LIMIT 1
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    Optional<LoanApplication> findNextAppliedLoanWithLock();

    /**
     * Fetch loan assigned to a specific agent for decision-making.
     */
    Optional<LoanApplication> findByLoanIdAndAssignedAgentId(Long loanId, Long agentId);
}
