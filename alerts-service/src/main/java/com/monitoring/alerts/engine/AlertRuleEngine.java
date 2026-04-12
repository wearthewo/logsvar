package com.monitoring.alerts.engine;

import com.monitoring.alerts.dto.AnomalyDto;
import com.monitoring.alerts.model.AlertRule;
import com.monitoring.alerts.repository.AlertRuleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AlertRuleEngine {
    
    private static final Logger logger = LoggerFactory.getLogger(AlertRuleEngine.class);
    
    @Autowired
    private AlertRuleRepository alertRuleRepository;
    
    public List<AlertRule> findMatchingRules(AnomalyDto anomaly) {
        try {
            // Find enabled rules that match the service name
            List<AlertRule> matchingRules = alertRuleRepository.findEnabledRulesForService(anomaly.getServiceName());
            
            // Filter by severity threshold
            return matchingRules.stream()
                .filter(rule -> rule.matchesSeverity(anomaly.getSeverity()))
                .toList();
            
        } catch (Exception e) {
            logger.error("Error finding matching rules for anomaly {}: {}", anomaly.getId(), e.getMessage());
            return List.of();
        }
    }
}
