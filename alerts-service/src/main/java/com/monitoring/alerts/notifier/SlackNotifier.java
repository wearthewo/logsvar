package com.monitoring.alerts.notifier;

import com.monitoring.alerts.dto.AnomalyDto;
import com.monitoring.alerts.model.AlertRule;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

@Component
public class SlackNotifier {
    
    private static final Logger logger = LoggerFactory.getLogger(SlackNotifier.class);
    
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    
    public SlackNotifier(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder.build();
        this.objectMapper = objectMapper;
    }
    
    public boolean sendAlert(AlertRule rule, AnomalyDto anomaly) {
        try {
            Map<String, Object> payload = Map.of(
                "text", String.format("*[%s]* Anomaly on `%s`\n%s\n*Action:* %s",
                                      anomaly.getSeverity(),
                                      anomaly.getServiceName(),
                                      anomaly.getReason(),
                                      anomaly.getRecommendedAction())
            );
            
            Mono<String> response = webClient.post()
                .uri(rule.getDestination())
                .header("Content-Type", "application/json")
                .bodyValue(objectMapper.writeValueAsString(payload))
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(5));
            
            String result = response.block();
            logger.info("Slack alert sent successfully to webhook for anomaly {}", anomaly.getId());
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to send Slack alert for anomaly {}: {}", 
                         anomaly.getId(), e.getMessage());
            return false;
        }
    }
}
