package com.turno.los.repository;

import com.turno.los.entity.Agent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AgentRepository extends JpaRepository<Agent, Long> {

    /**
     * Find one available agent using a pessimistic lock to prevent two threads
     * assigning the same agent simultaneously.
     */
    @Query(value = """
        SELECT * FROM agents
        WHERE available = true
        ORDER BY id ASC
        LIMIT 1
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    Optional<Agent> findAvailableAgentWithLock();
}
