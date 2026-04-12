package com.monitoring.backend.controller;

import com.monitoring.backend.model.AnomalyResponse;
import com.monitoring.backend.model.PageResponse;
import com.monitoring.backend.model.StatsResponse;
import com.monitoring.backend.service.AnomalyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/anomalies")
public class AnomalyController {
    
    private static final Logger logger = LoggerFactory.getLogger(AnomalyController.class);
    
    private final AnomalyService anomalyService;
    
    public AnomalyController(AnomalyService anomalyService) {
        this.anomalyService = anomalyService;
    }
    
    @GetMapping
    public ResponseEntity<PageResponse<AnomalyResponse>> getAnomalies(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String service,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        
        try {
            var pageable = org.springframework.data.domain.PageRequest.of(page, size);
            var anomalies = anomalyService.getAnomaliesWithFilters(service, severity, from, to, pageable);
            return ResponseEntity.ok(anomalies);
            
        } catch (IllegalArgumentException e) {
            logger.error("Invalid parameter: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Error fetching anomalies: {}", e.getMessage());
            throw new RuntimeException("Failed to fetch anomalies", e);
        }
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<AnomalyResponse> getAnomalyById(@PathVariable String id) {
        try {
            AnomalyResponse anomaly = anomalyService.getAnomalyById(id);
            if (anomaly == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(anomaly);
            
        } catch (Exception e) {
            logger.error("Error fetching anomaly by id {}: {}", id, e.getMessage());
            throw new RuntimeException("Failed to fetch anomaly", e);
        }
    }
    
    @GetMapping("/stats")
    public ResponseEntity<StatsResponse> getAnomalyStatistics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant since) {
        
        try {
            StatsResponse stats = anomalyService.getAnomalyStatistics(since);
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            logger.error("Error fetching anomaly statistics: {}", e.getMessage());
            throw new RuntimeException("Failed to fetch statistics", e);
        }
    }
}
