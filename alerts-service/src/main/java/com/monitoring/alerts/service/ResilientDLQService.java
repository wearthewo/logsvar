package com.monitoring.alerts.service;

import com.monitoring.alerts.dto.AnomalyDto;
import com.monitoring.alerts.model.AlertHistory;
import com.monitoring.alerts.model.AlertRule;
import com.monitoring.alerts.repository.AlertHistoryRepository;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class ResilientDLQService {

    private static final Logger logger = LoggerFactory.getLogger(ResilientDLQService.class);
    private static final Duration KAFKA_TIMEOUT = Duration.ofSeconds(2);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final AlertHistoryRepository alertHistoryRepository;
    private final CircuitBreaker kafkaCircuitBreaker;
    private final Retry kafkaRetry;
    private final CircuitBreaker mysqlCircuitBreaker;
    private final Retry mysqlRetry;
    private final com.monitoring.alerts.config.KafkaTopicsConfig kafkaTopicsConfig;

    public ResilientDLQService(KafkaTemplate<String, String> kafkaTemplate,
                               AlertHistoryRepository alertHistoryRepository,
                               CircuitBreaker kafkaCircuitBreaker,
                               Retry kafkaRetry,
                               CircuitBreaker mysqlCircuitBreaker,
                               Retry mysqlRetry,
                               com.monitoring.alerts.config.KafkaTopicsConfig kafkaTopicsConfig) {
        this.kafkaTemplate = kafkaTemplate;
        this.alertHistoryRepository = alertHistoryRepository;
        this.kafkaCircuitBreaker = kafkaCircuitBreaker;
        this.kafkaRetry = kafkaRetry;
        this.mysqlCircuitBreaker = mysqlCircuitBreaker;
        this.mysqlRetry = mysqlRetry;
        this.kafkaTopicsConfig = kafkaTopicsConfig;
    }

    public CompletableFuture<Boolean> publishToDLQ(AlertRule rule, AnomalyDto anomaly, String errorMessage) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // First, save to MySQL as fallback
                boolean savedToDb = saveFailedAlertToDatabase(rule, anomaly, errorMessage);
                
                // Then, try to publish to DLQ topic
                boolean publishedToKafka = kafkaRetry.executeSupplier(() -> 
                    kafkaCircuitBreaker.executeSupplier(() -> {
                        try {
                            String dlqMessage = createDLQMessage(rule, anomaly, errorMessage, savedToDb);
                            
                            kafkaTemplate.send(kafkaTopicsConfig.dlqTopic, rule.getId(), dlqMessage)
                                .whenComplete((result, ex) -> {
                                    if (ex == null) {
                                        logger.info("Successfully published alert {} to DLQ for anomaly {}", 
                                                   rule.getId(), anomaly.getId());
                                    } else {
                                        logger.error("Failed to publish alert {} to DLQ for anomaly {}: {}", 
                                                   rule.getId(), anomaly.getId(), ex.getMessage());
                                    }
                                });
                            
                            return true;
                        } catch (Exception e) {
                            logger.error("Failed to publish to DLQ for alert {} and anomaly {}: {}", 
                                       rule.getId(), anomaly.getId(), e.getMessage());
                            throw new RuntimeException("DLQ publish failed", e);
                        }
                    })
                );
                
                return savedToDb || publishedToKafka;
            } catch (Exception e) {
                logger.error("Circuit breaker open or retry exhausted for DLQ publish for alert {} and anomaly {}", 
                           rule.getId(), anomaly.getId(), e);
                return false;
            }
        });
    }

    private boolean saveFailedAlertToDatabase(AlertRule rule, AnomalyDto anomaly, String errorMessage) {
        try {
            return mysqlRetry.executeSupplier(() -> 
                mysqlCircuitBreaker.executeSupplier(() -> {
                    try {
                        AlertHistory alertHistory = new AlertHistory();
                        alertHistory.setAnomalyId(anomaly.getId());
                        alertHistory.setRuleId(rule.getId());
                        alertHistory.setChannel(rule.getChannel());
                        alertHistory.setDestination(rule.getDestination());
                        alertHistory.setStatus(AlertHistory.Status.FAILED);
                        alertHistory.setErrorMessage(errorMessage);
                        
                        alertHistoryRepository.save(alertHistory);
                        logger.info("Saved failed alert {} for anomaly {} to database", rule.getId(), anomaly.getId());
                        return true;
                    } catch (Exception e) {
                        logger.error("Failed to save failed alert to database for alert {} and anomaly {}: {}", 
                                   rule.getId(), anomaly.getId(), e.getMessage());
                        throw new RuntimeException("Database save failed", e);
                    }
                })
            );
        } catch (Exception e) {
            logger.error("Circuit breaker open or retry exhausted for database save for alert {} and anomaly {}", 
                       rule.getId(), anomaly.getId(), e);
            return false;
        }
    }

    private String createDLQMessage(AlertRule rule, AnomalyDto anomaly, String errorMessage, boolean savedToDb) {
        try {
            return String.format(
                "{\"id\":\"%s\",\"ruleId\":\"%s\",\"anomalyId\":\"%s\",\"channel\":\"%s\",\"destination\":\"%s\",\"errorMessage\":\"%s\",\"timestamp\":\"%s\",\"savedToDb\":%s}",
                UUID.randomUUID().toString(),
                rule.getId(),
                anomaly.getId(),
                rule.getChannel(),
                rule.getDestination(),
                errorMessage.replace("\"", "\\\""),
                LocalDateTime.now().toString(),
                savedToDb
            );
        } catch (Exception e) {
            logger.error("Failed to create DLQ message for alert {} and anomaly {}", rule.getId(), anomaly.getId(), e);
            throw new RuntimeException("DLQ message creation failed", e);
        }
    }

    public boolean isKafkaHealthy() {
        return kafkaCircuitBreaker.getState() == CircuitBreaker.State.CLOSED;
    }

    public boolean isDatabaseHealthy() {
        return mysqlCircuitBreaker.getState() == CircuitBreaker.State.CLOSED;
    }

    public CircuitBreaker.State getKafkaCircuitState() {
        return kafkaCircuitBreaker.getState();
    }

    public CircuitBreaker.State getDatabaseCircuitState() {
        return mysqlCircuitBreaker.getState();
    }
}
