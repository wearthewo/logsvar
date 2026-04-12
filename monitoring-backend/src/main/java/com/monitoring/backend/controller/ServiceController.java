package com.monitoring.backend.controller;

import com.monitoring.backend.model.ServiceSummary;
import com.monitoring.backend.service.ServiceHealthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/services")
public class ServiceController {
    
    private static final Logger logger = LoggerFactory.getLogger(ServiceController.class);
    
    @Autowired
    private ServiceHealthService serviceHealthService;
    
    @GetMapping
    public ResponseEntity<List<ServiceSummary>> getServices() {
        try {
            List<ServiceSummary> services = serviceHealthService.getServiceSummaries();
            return ResponseEntity.ok(services);
            
        } catch (Exception e) {
            logger.error("Error fetching service summaries: {}", e.getMessage());
            throw new RuntimeException("Failed to fetch services", e);
        }
    }
}
