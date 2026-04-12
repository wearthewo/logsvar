package com.monitoring.alerts.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "alert_rules")
public class AlertRule {
    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity_threshold", nullable = false)
    private SeverityThreshold severityThreshold;

    @Column(name = "service_filter")
    private String serviceFilter;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false)
    private Channel channel;

    @Column(name = "destination", columnDefinition = "TEXT")
    private String destination;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public enum SeverityThreshold {
        LOW, MEDIUM, HIGH, CRITICAL;
        
        public boolean isMetBy(String severity) {
            return this.ordinal() <= SeverityThreshold.valueOf(severity).ordinal();
        }
    }

    public enum Channel {
        EMAIL, SLACK, WEBHOOK, IN_APP
    }

    // Constructors
    public AlertRule() {}

    public AlertRule(String id, String name, Boolean enabled, SeverityThreshold severityThreshold, 
                   String serviceFilter, Channel channel, String destination) {
        this.id = id;
        this.name = name;
        this.enabled = enabled;
        this.severityThreshold = severityThreshold;
        this.serviceFilter = serviceFilter;
        this.channel = channel;
        this.destination = destination;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }

    public SeverityThreshold getSeverityThreshold() { return severityThreshold; }
    public void setSeverityThreshold(SeverityThreshold severityThreshold) { this.severityThreshold = severityThreshold; }

    public String getServiceFilter() { return serviceFilter; }
    public void setServiceFilter(String serviceFilter) { this.serviceFilter = serviceFilter; }

    public Channel getChannel() { return channel; }
    public void setChannel(Channel channel) { this.channel = channel; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
