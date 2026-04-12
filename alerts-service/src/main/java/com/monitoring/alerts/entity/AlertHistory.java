package com.monitoring.alerts.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "alert_history")
public class AlertHistory {
    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "anomaly_id", nullable = false)
    private String anomalyId;

    @Column(name = "rule_id", nullable = false)
    private String ruleId;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false)
    private AlertRule.Channel channel;

    @Column(name = "destination", columnDefinition = "TEXT")
    private String destination;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "sent_at", nullable = false)
    private Instant sentAt;

    public enum Status {
        SENT, FAILED, DEDUPLICATED
    }

    // Constructors
    public AlertHistory() {}

    public AlertHistory(String id, String anomalyId, String ruleId, AlertRule.Channel channel, 
                     String destination, Status status, String errorMessage) {
        this.id = id;
        this.anomalyId = anomalyId;
        this.ruleId = ruleId;
        this.channel = channel;
        this.destination = destination;
        this.status = status;
        this.errorMessage = errorMessage;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getAnomalyId() { return anomalyId; }
    public void setAnomalyId(String anomalyId) { this.anomalyId = anomalyId; }

    public String getRuleId() { return ruleId; }
    public void setRuleId(String ruleId) { this.ruleId = ruleId; }

    public AlertRule.Channel getChannel() { return channel; }
    public void setChannel(AlertRule.Channel channel) { this.channel = channel; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Instant getSentAt() { return sentAt; }
    public void setSentAt(Instant sentAt) { this.sentAt = sentAt; }
}
