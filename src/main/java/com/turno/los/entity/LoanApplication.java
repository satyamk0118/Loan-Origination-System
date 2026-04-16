package com.turno.los.entity;

import com.turno.los.enums.ApplicationStatus;
import com.turno.los.enums.LoanType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Core entity representing a customer's loan application.
 *
 * Indexed on:
 *  - application_status  (for fast status-based queries and the background processor)
 *  - customer_phone      (for customer lookup / top-customers query)
 */
@Entity
@Table(
    name = "loan_applications",
    indexes = {
        @Index(name = "idx_loan_status", columnList = "application_status"),
        @Index(name = "idx_loan_customer_phone", columnList = "customer_phone"),
        @Index(name = "idx_loan_assigned_agent", columnList = "assigned_agent_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long loanId;

    @Column(nullable = false)
    private String customerName;

    @Column(nullable = false)
    private String customerPhone;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal loanAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoanType loanType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ApplicationStatus applicationStatus = ApplicationStatus.APPLIED;

    /**
     * Agent assigned when the loan moves to UNDER_REVIEW.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_agent_id")
    private Agent assignedAgent;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
