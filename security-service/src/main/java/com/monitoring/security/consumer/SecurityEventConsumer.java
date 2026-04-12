package com.monitoring.security.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.monitoring.security.config.KafkaTopicsConfig;
import com.monitoring.security.dto.SecurityEventDto;
import com.monitoring.security.service.SecurityEventService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class SecurityEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(SecurityEventConsumer.class);
    private final ObjectMapper objectMapper;
    private final SecurityEventService securityEventService;
    private final KafkaTopicsConfig kafkaTopicsConfig;

    public SecurityEventConsumer(ObjectMapper objectMapper, 
                              SecurityEventService securityEventService,
                              KafkaTopicsConfig kafkaTopicsConfig) {
        this.objectMapper = objectMapper;
        this.securityEventService = securityEventService;
        this.kafkaTopicsConfig = kafkaTopicsConfig;
    }

    @KafkaListener(
        topics = "#{kafkaTopicsConfig.securityTopic}",
        groupId = "security-service",
        properties = {
            "enable.auto.commit=false"
        }
    )
    public void consumeSecurityEvent(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        try {
            String message = record.value();
            log.info("Received security event: {}", message);

            SecurityEventDto event = objectMapper.readValue(message, SecurityEventDto.class);
            securityEventService.processSecurityEvent(event);

            // Commit offset manually after successful processing
            acknowledgment.acknowledge();
            log.info("Successfully processed and acknowledged security event");

        } catch (Exception e) {
            log.error("Error processing security event: {}", e.getMessage(), e);
            // Don't acknowledge - let Kafka retry
        }
    }
}
