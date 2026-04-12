package com.monitoring.alerts.notifier;

import com.monitoring.alerts.dto.AnomalyDto;
import com.monitoring.alerts.model.AlertHistory;
import com.monitoring.alerts.model.AlertRule;
import com.monitoring.alerts.repository.AlertHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class InAppNotifier {
    
    private static final Logger logger = LoggerFactory.getLogger(InAppNotifier.class);
    
    @Autowired
    private AlertHistoryRepository alertHistoryRepository;
    
    @Transactional
    public AlertHistory sendAlert(String ruleId, AnomalyDto anomaly) {
        try {
            AlertHistory alertHistory = new AlertHistory();
            alertHistory.setAnomalyId(anomaly.getId());
            alertHistory.setRuleId(ruleId);
            alertHistory.setChannel(AlertRule.Channel.IN_APP);
            alertHistory.setStatus(AlertHistory.Status.SENT);
            
            AlertHistory saved = alertHistoryRepository.save(alertHistory);
            
            logger.info("In-app alert recorded for anomaly {} with rule {}", 
                        anomaly.getId(), ruleId);
            return saved;
            
        } catch (Exception e) {
            logger.error("Failed to record in-app alert for anomaly {}: {}", 
                         anomaly.getId(), e.getMessage());
            throw e;
        }
    }
}
