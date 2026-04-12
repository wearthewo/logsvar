package com.monitoring.alerts.config;

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
public class ResilienceConfig {

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
    public CircuitBreaker mysqlCircuitBreaker(CircuitBreakerRegistry registry) {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .slidingWindowSize(20)
                .minimumNumberOfCalls(10)
                .build();
        
        return registry.circuitBreaker("mysql", config);
    }

    @Bean
    public CircuitBreaker redisCircuitBreaker(CircuitBreakerRegistry registry) {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .slidingWindowSize(20)
                .minimumNumberOfCalls(10)
                .build();
        
        return registry.circuitBreaker("redis", config);
    }

    @Bean
    public CircuitBreaker smtpCircuitBreaker(CircuitBreakerRegistry registry) {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .slidingWindowSize(20)
                .minimumNumberOfCalls(10)
                .build();
        
        return registry.circuitBreaker("smtp", config);
    }

    @Bean
    public CircuitBreaker slackCircuitBreaker(CircuitBreakerRegistry registry) {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .slidingWindowSize(20)
                .minimumNumberOfCalls(10)
                .build();
        
        return registry.circuitBreaker("slack", config);
    }

    @Bean
    public CircuitBreaker webhookCircuitBreaker(CircuitBreakerRegistry registry) {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .slidingWindowSize(20)
                .minimumNumberOfCalls(10)
                .build();
        
        return registry.circuitBreaker("webhook", config);
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

    @Bean
    public Retry mysqlRetry(RetryRegistry registry) {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(500))
                .retryOnException(e -> true)
                .build();
        
        return registry.retry("mysql", config);
    }

    @Bean
    public Retry smtpRetry(RetryRegistry registry) {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(500))
                .retryOnException(e -> true)
                .build();
        
        return registry.retry("smtp", config);
    }

    @Bean
    public Retry slackRetry(RetryRegistry registry) {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(500))
                .retryOnException(e -> true)
                .build();
        
        return registry.retry("slack", config);
    }

    @Bean
    public Retry webhookRetry(RetryRegistry registry) {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(500))
                .retryOnException(e -> true)
                .build();
        
        return registry.retry("webhook", config);
    }
}
