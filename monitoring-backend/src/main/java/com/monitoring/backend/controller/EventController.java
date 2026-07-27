package com.monitoring.backend.controller;

import com.monitoring.backend.dto.EventRequest;
import com.monitoring.backend.service.EventService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class EventController {
    
    private static final Logger logger = LoggerFactory.getLogger(EventController.class);
    
    private final EventService eventService;
    
    public EventController(EventService eventService) {
        this.eventService = eventService;
    }
    
    @PostMapping("/events")
    public ResponseEntity<Map<String, String>> createEvent(@Valid @RequestBody EventRequest event) {
        String eventId = eventService.processEvent(event);
        logger.info("Accepted event submission: {}", eventId);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(Map.of("eventId", eventId, "status", "accepted"));
    }
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = Map.of(
            "status", "UP",
            "kafka", eventService.isKafkaHealthy(),
            "mysql", eventService.isMySqlHealthy()
        );
        
        boolean allHealthy = (Boolean) health.get("kafka") && (Boolean) health.get("mysql");
        HttpStatus status = allHealthy ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        
        return ResponseEntity.status(status).body(health);
    }
}
