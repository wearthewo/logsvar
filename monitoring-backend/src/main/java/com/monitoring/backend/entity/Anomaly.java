package com.monitoring.backend.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "anomalies")
public class Anomaly {
    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "event_id", nullable = false)
    private String eventId;

    @Column(name = "service_name", nullable = false)
    private String serviceName;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false)
    private Severity severity;

    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(name = "recommended_action", columnDefinition = "TEXT")
    private String recommendedAction;

    @CreationTimestamp
    @Column(name = "detected_at", nullable = false)
    private Instant detectedAt;

    public enum Severity {
        LOW, MEDIUM, HIGH, CRITICAL;
        
        public boolean isAtLeast(Severity other) {
            return this.ordinal() >= other.ordinal();
        }
    }

    // Constructors
    public Anomaly() {}

    public Anomaly(String id, String eventId, String serviceName, Severity severity, String reason, String recommendedAction) {
        this.id = id;
        this.eventId = eventId;
        this.serviceName = serviceName;
        this.severity = severity;
        this.reason = reason;
        this.recommendedAction = recommendedAction;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }

    public Severity getSeverity() { return severity; }
    public void setSeverity(Severity severity) { this.severity = severity; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getRecommendedAction() { return recommendedAction; }
    public void setRecommendedAction(String recommendedAction) { this.recommendedAction = recommendedAction; }

    public Instant getDetectedAt() { return detectedAt; }
    public void setDetectedAt(Instant detectedAt) { this.detectedAt = detectedAt; }
}
