package com.monitoring.alerts.controller;

import com.monitoring.alerts.model.AlertHistory;
import com.monitoring.alerts.repository.AlertHistoryRepository;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/alert-history")
public class AlertHistoryController {
    
    private static final Logger logger = LoggerFactory.getLogger(AlertHistoryController.class);
    
    private final AlertHistoryRepository alertHistoryRepository;
    
    public AlertHistoryController(AlertHistoryRepository alertHistoryRepository) {
        this.alertHistoryRepository = alertHistoryRepository;
    }
    
    @GetMapping
    public ResponseEntity<Page<AlertHistory>> getAlertHistory(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String ruleId,
            @RequestParam(required = false) AlertHistory.Status status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        
        Pageable pageable = PageRequest.of(page, size);
        List<AlertHistory> alerts;
        
        if (ruleId != null && from != null && to != null) {
            alerts = alertHistoryRepository.findByRuleIdAndSentAtBetween(ruleId, from, to);
        } else if (ruleId != null) {
            alerts = alertHistoryRepository.findByRuleId(ruleId);
        } else if (status != null) {
            alerts = alertHistoryRepository.findByStatus(status);
        } else if (from != null && to != null) {
            alerts = alertHistoryRepository.findBySentAtBetween(from, to);
        } else {
            return ResponseEntity.ok(alertHistoryRepository.findAll(pageable));
        }
        
        // Convert list to page
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), alerts.size());
        List<AlertHistory> pageContent = alerts.subList(start, end);
        
        Page<AlertHistory> result = new PageImpl<>(pageContent, pageable, alerts.size());
        
        return ResponseEntity.ok(result);
    }
}
