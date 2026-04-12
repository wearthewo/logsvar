package com.monitoring.backend.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.monitoring.backend.config.KafkaTopicsConfig;
import com.monitoring.backend.dto.EventDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class EventProducer {
    
    private static final Logger logger = LoggerFactory.getLogger(EventProducer.class);
    
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final KafkaTopicsConfig kafkaTopicsConfig;
    
    public EventProducer(KafkaTemplate<String, String> kafkaTemplate, 
                       ObjectMapper objectMapper,
                       KafkaTopicsConfig kafkaTopicsConfig) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.kafkaTopicsConfig = kafkaTopicsConfig;
    }
    
    public CompletableFuture<SendResult<String, String>> sendEvent(EventDto event) {
        try {
            String eventJson = objectMapper.writeValueAsString(event);
            
            logger.info("Sending event {} to topic {} with key {}", 
                       event.getId(), kafkaTopicsConfig.eventsTopic, event.getId());
            
            CompletableFuture<SendResult<String, String>> future = 
                kafkaTemplate.send(kafkaTopicsConfig.eventsTopic, event.getId(), eventJson);
            
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    logger.info("Event {} sent successfully to partition {} offset {}", 
                               event.getId(), 
                               result.getRecordMetadata().partition(),
                               result.getRecordMetadata().offset());
                } else {
                    logger.error("Failed to send event {}: {}", event.getId(), ex.getMessage());
                }
            });
            
            return future;
            
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize event {}: {}", event.getId(), e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }
    
    public boolean isKafkaHealthy() {
        try {
            kafkaTemplate.getProducerFactory().createProducer().close();
            return true;
        } catch (Exception e) {
            logger.warn("Kafka health check failed: {}", e.getMessage());
            return false;
        }
    }
}
