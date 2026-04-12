package com.monitoring.alerts.model;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Table(name = "alert_history")
@EntityListeners(AuditingEntityListener.class)
public class AlertHistory {
    
    @Id
    @Column(name = "id", length = 36)
    private String id;
    
    @Column(name = "anomaly_id", nullable = false, length = 36)
    private String anomalyId;
    
    @Column(name = "rule_id", nullable = false, length = 36)
    private String ruleId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 16)
    private AlertRule.Channel channel;
    
    @Column(name = "destination", columnDefinition = "TEXT")
    private String destination;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private Status status;
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    @CreatedDate
    @Column(name = "sent_at", nullable = false, columnDefinition = "DATETIME(3)")
    private Instant sentAt;
    
    public enum Status {
        SENT, FAILED, DEDUPLICATED
    }
    
    public AlertHistory() {
        this.id = java.util.UUID.randomUUID().toString();
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getAnomalyId() {
        return anomalyId;
    }
    
    public void setAnomalyId(String anomalyId) {
        this.anomalyId = anomalyId;
    }
    
    public String getRuleId() {
        return ruleId;
    }
    
    public void setRuleId(String ruleId) {
        this.ruleId = ruleId;
    }
    
    public AlertRule.Channel getChannel() {
        return channel;
    }
    
    public void setChannel(AlertRule.Channel channel) {
        this.channel = channel;
    }
    
    public String getDestination() {
        return destination;
    }
    
    public void setDestination(String destination) {
        this.destination = destination;
    }
    
    public Status getStatus() {
        return status;
    }
    
    public void setStatus(Status status) {
        this.status = status;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public Instant getSentAt() {
        return sentAt;
    }
    
    public void setSentAt(Instant sentAt) {
        this.sentAt = sentAt;
    }
}
