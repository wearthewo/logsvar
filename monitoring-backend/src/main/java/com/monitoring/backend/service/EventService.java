package com.monitoring.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.monitoring.backend.dto.EventDto;
import com.monitoring.backend.entity.MonitoringEvent;
import com.monitoring.backend.kafka.EventProducer;
import com.monitoring.backend.repository.MonitoringEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class EventService {
    
    private static final Logger logger = LoggerFactory.getLogger(EventService.class);
    
    private final MonitoringEventRepository eventRepository;
    private final EventProducer eventProducer;
    private final ObjectMapper objectMapper;
    
    public EventService(MonitoringEventRepository eventRepository, 
                       EventProducer eventProducer, 
                       ObjectMapper objectMapper) {
        this.eventRepository = eventRepository;
        this.eventProducer = eventProducer;
        this.objectMapper = objectMapper;
    }
    
    public CompletableFuture<String> processEvent(EventDto eventDto) {
        logger.info("Processing event {} of type {} from service {}", 
                   eventDto.getId(), eventDto.getEventType(), eventDto.getServiceName());
        
        try {
            // Save to MySQL
            MonitoringEvent monitoringEvent = convertToEntity(eventDto);
            eventRepository.save(monitoringEvent);
            
            logger.info("Saved event {} to database", eventDto.getId());
            
            // Send to Kafka
            CompletableFuture<String> kafkaFuture = eventProducer.sendEvent(eventDto)
                .thenApply(result -> {
                    logger.info("Event {} successfully published to Kafka", eventDto.getId());
                    return eventDto.getId();
                })
                .exceptionally(throwable -> {
                    logger.error("Failed to publish event {} to Kafka: {}", 
                                eventDto.getId(), throwable.getMessage());
                    throw new RuntimeException("Failed to publish event to Kafka", throwable);
                });
            
            return kafkaFuture;
            
        } catch (Exception e) {
            logger.error("Failed to process event {}: {}", eventDto.getId(), e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }
    
    private MonitoringEvent convertToEntity(EventDto eventDto) {
        try {
            String payloadJson = objectMapper.writeValueAsString(eventDto);
            
            MonitoringEvent entity = new MonitoringEvent();
            entity.setId(eventDto.getId());
            entity.setServiceName(eventDto.getServiceName());
            entity.setEventType(MonitoringEvent.EventType.valueOf(eventDto.getEventType().name()));
            entity.setPayload(payloadJson);
            
            return entity;
            
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize event payload", e);
        }
    }
    
    public boolean isKafkaHealthy() {
        return eventProducer.isKafkaHealthy();
    }
    
    public boolean isMySqlHealthy() {
        try {
            eventRepository.count();
            return true;
        } catch (Exception e) {
            logger.warn("MySQL health check failed: {}", e.getMessage());
            return false;
        }
    }
}
