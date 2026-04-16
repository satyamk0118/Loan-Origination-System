package com.turno.los.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Configures the fixed-size thread pool used by the loan processing background job.
 *
 * Thread count is externalized to application.properties so it can be tuned
 * per environment without a code change.
 */
@Configuration
public class ThreadPoolConfig {

    @Value("${loan.processing.thread-pool-size:5}")
    private int threadPoolSize;

    /**
     * Named bean to avoid ambiguity if more executors are added later.
     */
    @Bean(name = "loanProcessingExecutor", destroyMethod = "shutdown")
    public ExecutorService loanProcessingExecutor() {
        return Executors.newFixedThreadPool(threadPoolSize);
    }
}
