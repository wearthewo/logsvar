package com.monitoring.gateway.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class GatewayResilienceConfig {

    @Bean
    public CircuitBreaker monitoringBackendCircuitBreaker(CircuitBreakerRegistry registry) {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .slidingWindowSize(20)
                .minimumNumberOfCalls(10)
                .build();
        
        return registry.circuitBreaker("monitoring-backend", config);
    }

    @Bean
    public CircuitBreaker alertsServiceCircuitBreaker(CircuitBreakerRegistry registry) {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .slidingWindowSize(20)
                .minimumNumberOfCalls(10)
                .build();
        
        return registry.circuitBreaker("alerts-service", config);
    }

    @Bean
    public CircuitBreaker aiAgentCircuitBreaker(CircuitBreakerRegistry registry) {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .slidingWindowSize(20)
                .minimumNumberOfCalls(10)
                .build();
        
        return registry.circuitBreaker("ai-agent", config);
    }

    @Bean
    public CircuitBreaker kafkaCircuitBreaker(CircuitBreakerRegistry registry) {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .slidingWindowSize(20)
                .minimumNumberOfCalls(10)
                .build();

        return registry.circuitBreaker("kafka", config);
    }

    @Bean
    public Retry monitoringBackendRetry(RetryRegistry registry) {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(500))
                .retryOnException(e -> true)
                .build();
        
        return registry.retry("monitoring-backend", config);
    }

    @Bean
    public Retry alertsServiceRetry(RetryRegistry registry) {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(500))
                .retryOnException(e -> true)
                .build();
        
        return registry.retry("alerts-service", config);
    }

    @Bean
    public Retry aiAgentRetry(RetryRegistry registry) {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(500))
                .retryOnException(e -> true)
                .build();
        
        return registry.retry("ai-agent", config);
    }

    @Bean
    public Retry kafkaRetry(RetryRegistry registry) {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(500))
                .retryOnException(e -> true)
                .build();

        return registry.retry("kafka", config);
    }
}
