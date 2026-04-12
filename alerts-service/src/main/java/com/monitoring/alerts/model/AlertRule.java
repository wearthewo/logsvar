package com.monitoring.alerts.model;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Table(name = "alert_rules")
@EntityListeners(AuditingEntityListener.class)
public class AlertRule {
    
    @Id
    @Column(name = "id", length = 36)
    private String id;
    
    @Column(name = "name", nullable = false, length = 128)
    private String name;
    
    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "severity_threshold", nullable = false, length = 16)
    private SeverityThreshold severityThreshold;
    
    @Column(name = "service_filter", length = 128)
    private String serviceFilter;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 16)
    private Channel channel;
    
    @Column(name = "destination", columnDefinition = "TEXT")
    private String destination;
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, columnDefinition = "DATETIME(3)")
    private Instant createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME(3)")
    private Instant updatedAt;
    
    public AlertRule() {
        this.id = java.util.UUID.randomUUID().toString();
    }
    
    public enum SeverityThreshold {
        LOW, MEDIUM, HIGH, CRITICAL
    }
    
    public enum Channel {
        EMAIL, SLACK, WEBHOOK, IN_APP
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public Boolean getEnabled() {
        return enabled;
    }
    
    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
    
    public SeverityThreshold getSeverityThreshold() {
        return severityThreshold;
    }
    
    public void setSeverityThreshold(SeverityThreshold severityThreshold) {
        this.severityThreshold = severityThreshold;
    }
    
    public String getServiceFilter() {
        return serviceFilter;
    }
    
    public void setServiceFilter(String serviceFilter) {
        this.serviceFilter = serviceFilter;
    }
    
    public Channel getChannel() {
        return channel;
    }
    
    public void setChannel(Channel channel) {
        this.channel = channel;
    }
    
    public String getDestination() {
        return destination;
    }
    
    public void setDestination(String destination) {
        this.destination = destination;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
    
    public Instant getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public boolean matchesSeverity(String anomalySeverity) {
        if (anomalySeverity == null || this.severityThreshold == null) {
            return false;
        }
        
        int anomalyLevel = getSeverityLevel(anomalySeverity);
        int thresholdLevel = getSeverityLevel(this.severityThreshold.name());
        
        return anomalyLevel >= thresholdLevel;
    }
    
    public boolean matchesService(String serviceName) {
        if (serviceFilter == null || serviceFilter.trim().isEmpty()) {
            return true; // Apply to all services
        }
        return serviceFilter.equals(serviceName);
    }
    
    private int getSeverityLevel(String severity) {
        switch (severity) {
            case "LOW": return 1;
            case "MEDIUM": return 2;
            case "HIGH": return 3;
            case "CRITICAL": return 4;
            default: return 0;
        }
    }
}
