package com.monitoring.alerts.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.monitoring.alerts.config.KafkaTopicsConfig;
import com.monitoring.alerts.dto.AnomalyDto;
import com.monitoring.alerts.service.AlertProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
public class AnomalyConsumer {
    
    private static final Logger logger = LoggerFactory.getLogger(AnomalyConsumer.class);
    
    private final ObjectMapper objectMapper;
    private final AlertProcessingService alertProcessingService;
    private final KafkaTopicsConfig kafkaTopicsConfig;
    
    public AnomalyConsumer(ObjectMapper objectMapper, 
                          AlertProcessingService alertProcessingService,
                          KafkaTopicsConfig kafkaTopicsConfig) {
        this.objectMapper = objectMapper;
        this.alertProcessingService = alertProcessingService;
        this.kafkaTopicsConfig = kafkaTopicsConfig;
    }
    
    @KafkaListener(
        topics = "#{kafkaTopicsConfig.anomaliesTopic}",
        groupId = "alerts-service",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeAnomaly(
            @Payload String message,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {
        
        try {
            logger.info("Received anomaly from topic {} partition {} offset {}", topic, partition, offset);
            
            // Parse the anomaly message
            AnomalyDto anomaly = objectMapper.readValue(message, AnomalyDto.class);
            
            // Process the anomaly
            alertProcessingService.processAnomaly(anomaly);
            
            // Manually acknowledge the message
            acknowledgment.acknowledge();
            
            logger.info("Successfully processed and acknowledged anomaly {} from topic {}", 
                       anomaly.getId(), topic);
            
        } catch (Exception e) {
            logger.error("Error processing anomaly from topic {} partition {} offset {}: {}", 
                         topic, partition, offset, e.getMessage(), e);
            
            // Don't acknowledge the message to trigger retry
            // The message will be redelivered according to Kafka's retry policy
        }
    }
}
