package com.monitoring.alerts.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.monitoring.alerts.config.KafkaTopicsConfig;
import com.monitoring.alerts.dto.AlertHistoryDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class AlertProducer {
    
    private static final Logger logger = LoggerFactory.getLogger(AlertProducer.class);
    
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final KafkaTopicsConfig kafkaTopicsConfig;
    
    public AlertProducer(KafkaTemplate<String, String> kafkaTemplate,
                       ObjectMapper objectMapper,
                       KafkaTopicsConfig kafkaTopicsConfig) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.kafkaTopicsConfig = kafkaTopicsConfig;
    }
    
    public void publishAlert(AlertHistoryDto alert) {
        try {
            String jsonMessage = objectMapper.writeValueAsString(alert);
            kafkaTemplate.send(kafkaTopicsConfig.alertsTopic, alert.id(), jsonMessage)
                .whenComplete((result, failure) -> {
                    if (failure == null) {
                        logger.info("Published alert {} to topic {}", alert.id(), kafkaTopicsConfig.alertsTopic);
                    } else {
                        logger.error("Failed to publish alert {} to topic {}", alert.id(), kafkaTopicsConfig.alertsTopic, failure);
                    }
                });
        } catch (Exception e) {
            logger.error("Error serializing alert {}", alert.id(), e);
        }
    }
}
