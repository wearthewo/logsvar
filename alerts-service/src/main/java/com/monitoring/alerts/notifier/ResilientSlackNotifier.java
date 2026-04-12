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
public class ResilientSlackNotifier {

    private static final Logger logger = LoggerFactory.getLogger(ResilientSlackNotifier.class);
    private static final Duration WEBHOOK_TIMEOUT = Duration.ofSeconds(3);

    @Autowired
    private WebClient webClient;

    @Autowired
    private CircuitBreaker slackCircuitBreaker;

    @Autowired
    private Retry slackRetry;

    public CompletableFuture<Boolean> sendAlert(AlertRule rule, AnomalyDto anomaly) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return slackRetry.executeSupplier(() -> 
                    slackCircuitBreaker.executeSupplier(() -> {
                        try {
                            String webhookUrl = rule.getDestination();
                            String payload = createSlackPayload(rule, anomaly);
                            
                            String response = webClient.post()
                                .uri(webhookUrl)
                                .header("Content-Type", "application/json")
                                .bodyValue(payload)
                                .retrieve()
                                .bodyToMono(String.class)
                                .timeout(WEBHOOK_TIMEOUT)
                                .block();
                            
                            logger.info("Successfully sent Slack alert {} for anomaly {}", rule.getId(), anomaly.getId());
                            return true;
                        } catch (Exception e) {
                            logger.error("Failed to send Slack alert {} for anomaly {}: {}", 
                                       rule.getId(), anomaly.getId(), e.getMessage());
                            throw new RuntimeException("Slack send failed", e);
                        }
                    })
                );
            } catch (Exception e) {
                logger.error("Circuit breaker open or retry exhausted for Slack alert {} and anomaly {}", 
                           rule.getId(), anomaly.getId(), e);
                return false;
            }
        });
    }

    private String createSlackPayload(AlertRule rule, AnomalyDto anomaly) {
        return String.format(
            "{" +
            "\"text\":\"Alert: %s - %s\"," +
            "\"attachments\":[" +
            "{" +
            "\"color\":\"%s\"," +
            "\"fields\":[" +
            "{" +
            "\"title\":\"Service\"," +
            "\"value\":\"%s\"," +
            "\"short\":true" +
            "}," +
            "{" +
            "\"title\":\"Severity\"," +
            "\"value\":\"%s\"," +
            "\"short\":true" +
            "}," +
            "{" +
            "\"title\":\"Anomaly ID\"," +
            "\"value\":\"%s\"," +
            "\"short\":false" +
            "}," +
            "{" +
            "\"title\":\"Reason\"," +
            "\"value\":\"%s\"," +
            "\"short\":false" +
            "}," +
            "{" +
            "\"title\":\"Recommended Action\"," +
            "\"value\":\"%s\"," +
            "\"short\":false" +
            "}" +
            "]," +
            "\"ts\":\"%s\"" +
            "}" +
            "]" +
            "}",
            anomaly.getServiceName(),
            anomaly.getSeverity(),
            getSeverityColor(anomaly.getSeverity()),
            anomaly.getServiceName(),
            anomaly.getSeverity(),
            anomaly.getId(),
            anomaly.getReason(),
            anomaly.getRecommendedAction(),
            anomaly.getDetectedAt()
        );
    }

    private String getSeverityColor(String severity) {
        switch (severity.toUpperCase()) {
            case "CRITICAL":
                return "danger";
            case "HIGH":
                return "warning";
            case "MEDIUM":
                return "good";
            case "LOW":
                return "#36a64f";
            default:
                return "good";
        }
    }

    public boolean isSlackHealthy() {
        return slackCircuitBreaker.getState() == CircuitBreaker.State.CLOSED;
    }

    public CircuitBreaker.State getSlackCircuitState() {
        return slackCircuitBreaker.getState();
    }
}
