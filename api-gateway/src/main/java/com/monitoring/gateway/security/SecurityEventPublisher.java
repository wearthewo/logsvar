package com.monitoring.gateway.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.monitoring.gateway.config.KafkaTopicsConfig;
import com.monitoring.gateway.filter.SecurityAuditFilter;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class SecurityEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(SecurityEventPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final CircuitBreaker kafkaCircuitBreaker;
    private final Retry kafkaRetry;
    private final ObjectMapper objectMapper;
    private final KafkaTopicsConfig kafkaTopicsConfig;

    public SecurityEventPublisher(KafkaTemplate<String, String> kafkaTemplate,
                                 @Qualifier("kafkaCircuitBreaker") CircuitBreaker kafkaCircuitBreaker,
                                 @Qualifier("kafkaRetry") Retry kafkaRetry,
                                 ObjectMapper objectMapper,
                                 KafkaTopicsConfig kafkaTopicsConfig) {
        this.kafkaTemplate = kafkaTemplate;
        this.kafkaCircuitBreaker = kafkaCircuitBreaker;
        this.kafkaRetry = kafkaRetry;
        this.objectMapper = objectMapper;
        this.kafkaTopicsConfig = kafkaTopicsConfig;
    }

    public void publishSecurityEvent(SecurityAuditFilter.SecurityEvent event) {
        try {
            String eventJson = objectMapper.writeValueAsString(event);
            
            CompletableFuture.runAsync(() -> {
                try {
                    kafkaRetry.executeRunnable(() -> 
                        kafkaCircuitBreaker.executeRunnable(() -> {
                            try {
                                kafkaTemplate.send(kafkaTopicsConfig.securityTopic, event.getUserId(), eventJson)
                                    .whenComplete((result, ex) -> {
                                        if (ex != null) {
                                            logger.error("Failed to publish security event: {}", ex.getMessage());
                                        } else {
                                            logger.debug("Security event published successfully: {}", event.getType());
                                        }
                                    });
                            } catch (Exception e) {
                                logger.error("Error publishing security event: {}", e.getMessage());
                                throw new RuntimeException("Security event publish failed", e);
                            }
                        })
                    );
                } catch (Exception e) {
                    logger.error("Circuit breaker open or retry exhausted for security event: {}", e.getMessage());
                }
            });
            
        } catch (Exception e) {
            logger.error("Failed to serialize security event: {}", e.getMessage());
        }
    }

    public void publishAuditEvent(SecurityAuditFilter.SecurityAuditEvent auditEvent) {
        try {
            String auditJson = objectMapper.writeValueAsString(auditEvent);
            
            CompletableFuture.runAsync(() -> {
                try {
                    kafkaRetry.executeRunnable(() -> 
                        kafkaCircuitBreaker.executeRunnable(() -> {
                            try {
                                kafkaTemplate.send(kafkaTopicsConfig.auditTopic, auditEvent.getUserId(), auditJson)
                                    .whenComplete((result, ex) -> {
                                        if (ex != null) {
                                            logger.error("Failed to publish security audit event: {}", ex.getMessage());
                                        } else {
                                            logger.debug("Security audit event published successfully: {}", auditEvent.getAction());
                                        }
                                    });
                            } catch (Exception e) {
                                logger.error("Error publishing security audit event: {}", e.getMessage());
                                throw new RuntimeException("Security audit event publish failed", e);
                            }
                        })
                    );
                } catch (Exception e) {
                    logger.error("Circuit breaker open or retry exhausted for security audit event: {}", e.getMessage());
                }
            });
            
        } catch (Exception e) {
            logger.error("Failed to serialize security audit event: {}", e.getMessage());
        }
    }
}
