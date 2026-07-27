package com.monitoring.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.monitoring.backend.dto.EventEnvelope;
import com.monitoring.backend.dto.EventRequest;
import com.monitoring.backend.entity.MonitoringEvent;
import com.monitoring.backend.kafka.EventProducer;
import com.monitoring.backend.repository.MonitoringEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

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
    
    public String processEvent(EventRequest request) {
        EventEnvelope eventDto = new EventEnvelope(
            request.id() == null || request.id().isBlank() ? UUID.randomUUID().toString() : request.id(),
            request.serviceName(),
            request.eventType(),
            request.timestamp() == null ? Instant.now() : request.timestamp(),
            request.payload()
        );
        logger.info("Processing event {} of type {} from service {}", 
                   eventDto.id(), eventDto.eventType(), eventDto.serviceName());
        
        try {
            // Save to MySQL
            MonitoringEvent monitoringEvent = convertToEntity(eventDto);
            eventRepository.save(monitoringEvent);
            
            logger.info("Saved event {} to database", eventDto.id());
            
            // Send to Kafka
            eventProducer.sendEvent(eventDto).whenComplete((result, throwable) -> {
                if (throwable != null) {
                    logger.error("Failed to publish event {} to Kafka: {}", eventDto.id(), throwable.getMessage());
                }
            });
            return eventDto.id();
            
        } catch (Exception e) {
            logger.error("Failed to process event {}: {}", eventDto.id(), e.getMessage());
            throw new RuntimeException("Failed to persist event", e);
        }
    }
    
    private MonitoringEvent convertToEntity(EventEnvelope eventDto) {
        try {
            String payloadJson = objectMapper.writeValueAsString(eventDto);
            
            MonitoringEvent entity = new MonitoringEvent();
            entity.setId(eventDto.id());
            entity.setServiceName(eventDto.serviceName());
            entity.setEventType(MonitoringEvent.EventType.valueOf(eventDto.eventType().name()));
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
