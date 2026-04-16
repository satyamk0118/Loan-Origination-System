package com.turno.los;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the Loan Origination System (LOS).
 *
 * Enables Spring Boot auto-configuration and scheduling support
 * for the background loan processing job.
 */
@SpringBootApplication
@EnableScheduling
public class LoanOriginationSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(LoanOriginationSystemApplication.class, args);
    }
}
