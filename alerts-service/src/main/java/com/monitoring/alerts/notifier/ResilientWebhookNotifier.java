package com.monitoring.alerts.notifier;

import com.monitoring.alerts.dto.AnomalyDto;
import com.monitoring.alerts.model.AlertRule;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

@Service
public class ResilientWebhookNotifier {

    private static final Logger logger = LoggerFactory.getLogger(ResilientWebhookNotifier.class);
    private static final Duration WEBHOOK_TIMEOUT = Duration.ofSeconds(3);

    @Autowired
    private WebClient webClient;

    @Autowired
    private CircuitBreaker webhookCircuitBreaker;

    @Autowired
    private Retry webhookRetry;

    public CompletableFuture<Boolean> sendAlert(AlertRule rule, AnomalyDto anomaly) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return webhookRetry.executeSupplier(() -> 
                    webhookCircuitBreaker.executeSupplier(() -> {
                        try {
                            String webhookUrl = rule.getDestination();
                            String payload = createWebhookPayload(rule, anomaly);
                            
                            String response = webClient.post()
                                .uri(webhookUrl)
                                .header("Content-Type", "application/json")
                                .header("User-Agent", "Monitoring-Alerts-Service/1.0")
                                .bodyValue(payload)
                                .retrieve()
                                .bodyToMono(String.class)
                                .timeout(WEBHOOK_TIMEOUT)
                                .block();
                            
                            logger.info("Successfully sent webhook alert {} for anomaly {}", rule.getId(), anomaly.getId());
                            return true;
                        } catch (Exception e) {
                            logger.error("Failed to send webhook alert {} for anomaly {}: {}", 
                                       rule.getId(), anomaly.getId(), e.getMessage());
                            throw new RuntimeException("Webhook send failed", e);
                        }
                    })
                );
            } catch (Exception e) {
                logger.error("Circuit breaker open or retry exhausted for webhook alert {} and anomaly {}", 
                           rule.getId(), anomaly.getId(), e);
                return false;
            }
        });
    }

    private String createWebhookPayload(AlertRule rule, AnomalyDto anomaly) {
        return String.format(
            "{" +
            "\"alertId\":\"%s\"," +
            "\"ruleId\":\"%s\"," +
            "\"ruleName\":\"%s\"," +
            "\"anomalyId\":\"%s\"," +
            "\"serviceName\":\"%s\"," +
            "\"severity\":\"%s\"," +
            "\"reason\":\"%s\"," +
            "\"recommendedAction\":\"%s\"," +
            "\"detectedAt\":\"%s\"," +
            "\"channel\":\"%s\"," +
            "\"destination\":\"%s\"," +
            "\"timestamp\":\"%s\"" +
            "}",
            java.util.UUID.randomUUID().toString(),
            rule.getId(),
            rule.getName(),
            anomaly.getId(),
            anomaly.getServiceName(),
            anomaly.getSeverity(),
            anomaly.getReason().replace("\"", "\\\""),
            anomaly.getRecommendedAction().replace("\"", "\\\""),
            anomaly.getDetectedAt(),
            rule.getChannel(),
            rule.getDestination(),
            java.time.LocalDateTime.now().toString()
        );
    }

    public boolean isWebhookHealthy() {
        return webhookCircuitBreaker.getState() == CircuitBreaker.State.CLOSED;
    }

    public CircuitBreaker.State getWebhookCircuitState() {
        return webhookCircuitBreaker.getState();
    }
}
