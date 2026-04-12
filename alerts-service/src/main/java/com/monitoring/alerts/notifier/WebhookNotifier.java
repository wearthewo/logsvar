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

@Component
public class WebhookNotifier {
    
    private static final Logger logger = LoggerFactory.getLogger(WebhookNotifier.class);
    
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    
    public WebhookNotifier(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder.build();
        this.objectMapper = objectMapper;
    }
    
    public boolean sendAlert(AlertRule rule, AnomalyDto anomaly) {
        try {
            Mono<String> response = webClient.post()
                .uri(rule.getDestination())
                .header("Content-Type", "application/json")
                .bodyValue(objectMapper.writeValueAsString(anomaly))
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(5));
            
            String result = response.block();
            logger.info("Webhook alert sent successfully to {} for anomaly {}", 
                        rule.getDestination(), anomaly.getId());
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to send webhook alert to {} for anomaly {}: {}", 
                         rule.getDestination(), anomaly.getId(), e.getMessage());
            return false;
        }
    }
}
