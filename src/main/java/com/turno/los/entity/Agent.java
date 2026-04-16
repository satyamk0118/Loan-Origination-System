package com.turno.los.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

/**
 * Represents a loan review agent.
 *
 * Self-referential hierarchy: each agent optionally belongs to a manager
 * who is also an Agent (supporting the rule "an agent can be the manager
 * of another agent").
 */
@Entity
@Table(name = "agents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Agent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    /**
     * The manager of this agent. Null if this agent is a top-level manager.
     * An agent CAN be a manager of other agents (self-referential).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private Agent manager;

    /**
     * Agents who report to this agent (when this agent acts as a manager).
     */
    @OneToMany(mappedBy = "manager", fetch = FetchType.LAZY)
    private List<Agent> subordinates;

    /**
     * Loans currently assigned to this agent.
     */
    @OneToMany(mappedBy = "assignedAgent", fetch = FetchType.LAZY)
    private List<LoanApplication> assignedLoans;

    /**
     * Indicates whether the agent is available for new assignments.
     * In a real system this would track workload; here it's a simple flag.
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean available = true;
}
