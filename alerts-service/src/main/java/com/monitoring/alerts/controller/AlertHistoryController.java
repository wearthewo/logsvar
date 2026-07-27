package com.monitoring.alerts.controller;

import com.monitoring.alerts.model.AlertHistory;
import com.monitoring.alerts.model.PageResponse;
import com.monitoring.alerts.repository.AlertHistoryRepository;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/alert-history")
public class AlertHistoryController {
    
    private static final Logger logger = LoggerFactory.getLogger(AlertHistoryController.class);
    
    private final AlertHistoryRepository alertHistoryRepository;
    
    public AlertHistoryController(AlertHistoryRepository alertHistoryRepository) {
        this.alertHistoryRepository = alertHistoryRepository;
    }
    
    @GetMapping
    public ResponseEntity<PageResponse<AlertHistory>> getAlertHistory(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String ruleId,
            @RequestParam(required = false) AlertHistory.Status status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "sentAt"));
        Page<AlertHistory> result = alertHistoryRepository.findByFilters(ruleId, status, from, to, pageable);
        return ResponseEntity.ok(new PageResponse<>(result.getContent(), result.getTotalElements(),
                result.getTotalPages(), result.getNumber(), result.getSize()));
    }
}
