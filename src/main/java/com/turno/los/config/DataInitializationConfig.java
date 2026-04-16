package com.turno.los.config;

import com.turno.los.entity.Agent;
import com.turno.los.repository.AgentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Seeds initial agent data on first startup if the table is empty.
 *
 * Hierarchy:
 *   Alice (manager, top-level)
 *     └── Bob   (agent, reports to Alice; Alice is also an agent)
 *     └── Carol (agent, reports to Alice)
 *   Dave (manager, top-level)
 *     └── Eve   (agent, reports to Dave)
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializationConfig {

    private final AgentRepository agentRepository;

    @Bean
    public CommandLineRunner seedAgents() {
        return args -> {
            if (agentRepository.count() > 0) {
                log.info("Agents already seeded — skipping initialization.");
                return;
            }

            // Top-level managers (they are also agents themselves)
            Agent alice = agentRepository.save(
                Agent.builder().name("Alice Johnson").email("alice@los.com").available(true).build()
            );
            Agent dave = agentRepository.save(
                Agent.builder().name("Dave Wilson").email("dave@los.com").available(true).build()
            );

            // Agents under Alice
            agentRepository.save(
                Agent.builder().name("Bob Smith").email("bob@los.com").manager(alice).available(true).build()
            );
            agentRepository.save(
                Agent.builder().name("Carol White").email("carol@los.com").manager(alice).available(true).build()
            );

            // Agent under Dave
            agentRepository.save(
                Agent.builder().name("Eve Davis").email("eve@los.com").manager(dave).available(true).build()
            );

            log.info("Seeded 5 agents (2 managers + 3 agents).");
        };
    }
}
