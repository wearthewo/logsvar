package com.monitoring.alerts.service;

import com.monitoring.alerts.dto.AnomalyDto;
import com.monitoring.alerts.engine.AlertRuleEngine;
import com.monitoring.alerts.metrics.AlertsMetrics;
import com.monitoring.alerts.model.AlertHistory;
import com.monitoring.alerts.model.AlertRule;
import com.monitoring.alerts.notifier.*;
import com.monitoring.alerts.repository.AlertHistoryRepository;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class AlertProcessingService {
    
    private static final Logger logger = LoggerFactory.getLogger(AlertProcessingService.class);
    
    private final AlertRuleEngine alertRuleEngine;
    private final DeduplicationService deduplicationService;
    private final InAppNotifier inAppNotifier;
    private final AlertHistoryRepository alertHistoryRepository;
    private final AlertsMetrics alertsMetrics;
    private final ResilientDLQService resilientDLQService;
    private final ResilientEmailNotifier resilientEmailNotifier;
    private final ResilientSlackNotifier resilientSlackNotifier;
    private final ResilientWebhookNotifier resilientWebhookNotifier;
    
    public AlertProcessingService(AlertRuleEngine alertRuleEngine,
                                  DeduplicationService deduplicationService,
                                  InAppNotifier inAppNotifier,
                                  AlertHistoryRepository alertHistoryRepository,
                                  AlertsMetrics alertsMetrics,
                                  ResilientDLQService resilientDLQService,
                                  ResilientEmailNotifier resilientEmailNotifier,
                                  ResilientSlackNotifier resilientSlackNotifier,
                                  ResilientWebhookNotifier resilientWebhookNotifier) {
        this.alertRuleEngine = alertRuleEngine;
        this.deduplicationService = deduplicationService;
        this.inAppNotifier = inAppNotifier;
        this.alertHistoryRepository = alertHistoryRepository;
        this.alertsMetrics = alertsMetrics;
        this.resilientDLQService = resilientDLQService;
        this.resilientEmailNotifier = resilientEmailNotifier;
        this.resilientSlackNotifier = resilientSlackNotifier;
        this.resilientWebhookNotifier = resilientWebhookNotifier;
    }
    
    public void processAnomaly(AnomalyDto anomaly) {
        // Start timing the alert processing
        Timer.Sample timerSample = alertsMetrics.startTimer();
        
        logger.info("Processing anomaly {} from service {} with severity {}", 
                   anomaly.getId(), anomaly.getServiceName(), anomaly.getSeverity());
        
        try {
            // Check for deduplication
            if (deduplicationService.isDuplicated(anomaly.getServiceName(), anomaly.getSeverity())) {
                logger.info("Anomaly {} is duplicated, skipping", anomaly.getId());
                
                // Increment deduplication counter
                alertsMetrics.incrementAlertsDeduplicated();
                
                // Record deduplication in history
                recordDeduplicatedAlerts(anomaly);
                return;
            }
        
        // Find matching rules
        List<AlertRule> matchingRules = alertRuleEngine.findMatchingRules(anomaly);
        
        if (matchingRules.isEmpty()) {
            logger.info("No matching rules found for anomaly {}", anomaly.getId());
            return;
        }
        
        logger.info("Found {} matching rules for anomaly {}", matchingRules.size(), anomaly.getId());
        
        // Set deduplication key to prevent duplicate alerts
        deduplicationService.setDeduplicationKey(anomaly.getServiceName(), anomaly.getSeverity());
        
        // Process each matching rule
        for (AlertRule rule : matchingRules) {
            processAlertRule(rule, anomaly);
        }
        
        } finally {
            // Stop timing the alert processing
            alertsMetrics.stopTimer(timerSample);
        }
    }
    
    private CompletableFuture<Void> processAlertRule(AlertRule rule, AnomalyDto anomaly) {
        return CompletableFuture.runAsync(() -> {
            try {
                boolean success = false;
                String errorMessage = null;
                
                switch (rule.getChannel()) {
                    case EMAIL:
                        CompletableFuture<Boolean> emailFuture = resilientEmailNotifier.sendAlert(rule, anomaly);
                        success = emailFuture.join();
                        if (!success) {
                            errorMessage = "Email notification failed after retries";
                        }
                        break;
                        
                    case SLACK:
                        CompletableFuture<Boolean> slackFuture = resilientSlackNotifier.sendAlert(rule, anomaly);
                        success = slackFuture.join();
                        if (!success) {
                            errorMessage = "Slack notification failed after retries";
                        }
                        break;
                        
                    case WEBHOOK:
                        CompletableFuture<Boolean> webhookFuture = resilientWebhookNotifier.sendAlert(rule, anomaly);
                        success = webhookFuture.join();
                        if (!success) {
                            errorMessage = "Webhook notification failed after retries";
                        }
                        break;
                        
                    case IN_APP:
                        AlertHistory alertHistory = inAppNotifier.sendAlert(rule.getId(), anomaly);
                        success = true;
                        break;
                        
                    default:
                        logger.warn("Unknown channel {} for rule {}", rule.getChannel(), rule.getId());
                        return;
                }
                
                // Record alert in history
                recordAlertInHistory(rule, anomaly, success, errorMessage);
                
                // Track metrics
                if (success) {
                    alertsMetrics.incrementAlertsSent();
                } else {
                    alertsMetrics.incrementAlertsFailed();
                    
                    // If alert failed after retries, publish to DLQ
                    logger.info("Alert {} failed for anomaly {}, publishing to DLQ", rule.getId(), anomaly.getId());
                    CompletableFuture<Boolean> dlqFuture = resilientDLQService.publishToDLQ(rule, anomaly, errorMessage);
                    dlqFuture.whenComplete((dlqSuccess, ex) -> {
                        if (ex != null) {
                            logger.error("Failed to publish alert {} to DLQ for anomaly {}: {}", 
                                       rule.getId(), anomaly.getId(), ex.getMessage());
                        } else if (!dlqSuccess) {
                            logger.error("DLQ publish failed for alert {} and anomaly {}", rule.getId(), anomaly.getId());
                        } else {
                            logger.info("Successfully published alert {} to DLQ for anomaly {}", rule.getId(), anomaly.getId());
                        }
                    });
                }
                
                logger.info("Alert rule {} processed for anomaly {}: {}", 
                           rule.getId(), anomaly.getId(), success ? "SUCCESS" : "FAILED");
                
            } catch (Exception e) {
                logger.error("Error processing alert rule {} for anomaly {}: {}", 
                             rule.getId(), anomaly.getId(), e.getMessage());
                
                // Track failure metrics
                alertsMetrics.incrementAlertsFailed();
                
                // Publish to DLQ on exception
                CompletableFuture<Boolean> dlqFuture = resilientDLQService.publishToDLQ(rule, anomaly, e.getMessage());
                dlqFuture.whenComplete((dlqSuccess, ex) -> {
                    if (ex != null) {
                        logger.error("Failed to publish alert {} to DLQ for anomaly {}: {}", 
                                   rule.getId(), anomaly.getId(), ex.getMessage());
                    } else if (!dlqSuccess) {
                        logger.error("DLQ publish failed for alert {} and anomaly {}", rule.getId(), anomaly.getId());
                    } else {
                        logger.info("Successfully published alert {} to DLQ for anomaly {}", rule.getId(), anomaly.getId());
                    }
                });
                
                recordAlertInHistory(rule, anomaly, false, e.getMessage());
            }
        });
    }
    
    private void recordAlertInHistory(AlertRule rule, AnomalyDto anomaly, boolean success, String errorMessage) {
        try {
            AlertHistory alertHistory = new AlertHistory();
            alertHistory.setAnomalyId(anomaly.getId());
            alertHistory.setRuleId(rule.getId());
            alertHistory.setChannel(rule.getChannel());
            alertHistory.setDestination(rule.getDestination());
            alertHistory.setStatus(success ? AlertHistory.Status.SENT : AlertHistory.Status.FAILED);
            alertHistory.setErrorMessage(errorMessage);
            
            alertHistoryRepository.save(alertHistory);
            
        } catch (Exception e) {
            logger.error("Failed to record alert in history for rule {} and anomaly {}: {}", 
                         rule.getId(), anomaly.getId(), e.getMessage());
        }
    }
    
    private void recordDeduplicatedAlerts(AnomalyDto anomaly) {
        try {
            List<AlertRule> allMatchingRules = alertRuleEngine.findMatchingRules(anomaly);
            
            for (AlertRule rule : allMatchingRules) {
                AlertHistory alertHistory = new AlertHistory();
                alertHistory.setAnomalyId(anomaly.getId());
                alertHistory.setRuleId(rule.getId());
                alertHistory.setChannel(rule.getChannel());
                alertHistory.setDestination(rule.getDestination());
                alertHistory.setStatus(AlertHistory.Status.DEDUPLICATED);
                
                alertHistoryRepository.save(alertHistory);
            }
            
        } catch (Exception e) {
            logger.error("Failed to record deduplicated alerts for anomaly {}: {}", 
                         anomaly.getId(), e.getMessage());
        }
    }
}
