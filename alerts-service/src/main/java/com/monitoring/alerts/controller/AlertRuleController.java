package com.monitoring.alerts.controller;

import com.monitoring.alerts.dto.AlertRuleRequest;
import com.monitoring.alerts.dto.AlertRuleResponse;
import com.monitoring.alerts.model.AlertRule;
import com.monitoring.alerts.repository.AlertRuleRepository;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/alert-rules")
public class AlertRuleController {
    
    private static final Logger logger = LoggerFactory.getLogger(AlertRuleController.class);
    
    private final AlertRuleRepository alertRuleRepository;
    
    public AlertRuleController(AlertRuleRepository alertRuleRepository) {
        this.alertRuleRepository = alertRuleRepository;
    }
    
    @GetMapping
    public ResponseEntity<List<AlertRuleResponse>> getAllRules() {
        List<AlertRule> rules = alertRuleRepository.findAll();
        List<AlertRuleResponse> responses = rules.stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }
    
    @PostMapping
    public ResponseEntity<AlertRuleResponse> createRule(@Valid @RequestBody AlertRuleRequest request) {
        AlertRule rule = convertToEntity(request);
        rule.setId(UUID.randomUUID().toString());
        
        AlertRule saved = alertRuleRepository.save(rule);
        logger.info("Created alert rule: {}", saved.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(convertToResponse(saved));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<AlertRuleResponse> getRule(@PathVariable String id) {
        return alertRuleRepository.findById(id)
            .map(rule -> ResponseEntity.ok(convertToResponse(rule)))
            .orElse(ResponseEntity.notFound().build());
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<AlertRuleResponse> updateRule(@PathVariable String id, @Valid @RequestBody AlertRuleRequest request) {
        return alertRuleRepository.findById(id)
            .map(rule -> {
                updateEntityFromRequest(rule, request);
                AlertRule updated = alertRuleRepository.save(rule);
                logger.info("Updated alert rule: {}", updated.getId());
                return ResponseEntity.ok(convertToResponse(updated));
            })
            .orElse(ResponseEntity.notFound().build());
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteRule(@PathVariable String id) {
        return alertRuleRepository.findById(id)
            .map(rule -> {
                alertRuleRepository.delete(rule);
                logger.info("Deleted alert rule: {}", id);
                return ResponseEntity.ok(Map.of("status", "deleted", "id", id));
            })
            .orElse(ResponseEntity.notFound().build());
    }
    
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<AlertRuleResponse> toggleRule(@PathVariable String id) {
        return alertRuleRepository.findById(id)
            .map(rule -> {
                rule.setEnabled(!rule.getEnabled());
                AlertRule updated = alertRuleRepository.save(rule);
                logger.info("Toggled alert rule {} to {}", id, updated.getEnabled());
                return ResponseEntity.ok(convertToResponse(updated));
            })
            .orElse(ResponseEntity.notFound().build());
    }
    
    private AlertRule convertToEntity(AlertRuleRequest request) {
        AlertRule rule = new AlertRule();
        rule.setName(request.getName());
        rule.setEnabled(request.getEnabled());
        rule.setSeverityThreshold(AlertRule.SeverityThreshold.valueOf(request.getSeverityThreshold().name()));
        rule.setServiceFilter(request.getServiceFilter());
        rule.setChannel(AlertRule.Channel.valueOf(request.getChannel().name()));
        rule.setDestination(request.getDestination());
        return rule;
    }
    
    private void updateEntityFromRequest(AlertRule rule, AlertRuleRequest request) {
        rule.setName(request.getName());
        rule.setEnabled(request.getEnabled());
        rule.setSeverityThreshold(AlertRule.SeverityThreshold.valueOf(request.getSeverityThreshold().name()));
        rule.setServiceFilter(request.getServiceFilter());
        rule.setChannel(AlertRule.Channel.valueOf(request.getChannel().name()));
        rule.setDestination(request.getDestination());
    }
    
    private AlertRuleResponse convertToResponse(AlertRule rule) {
        AlertRuleResponse response = new AlertRuleResponse();
        response.setId(rule.getId());
        response.setName(rule.getName());
        response.setEnabled(rule.getEnabled());
        response.setSeverityThreshold(AlertRuleResponse.SeverityThreshold.valueOf(rule.getSeverityThreshold().name()));
        response.setServiceFilter(rule.getServiceFilter());
        response.setChannel(AlertRuleResponse.Channel.valueOf(rule.getChannel().name()));
        response.setDestination(rule.getDestination());
        response.setCreatedAt(rule.getCreatedAt());
        response.setUpdatedAt(rule.getUpdatedAt());
        return response;
    }
}
