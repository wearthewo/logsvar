package com.monitoring.alerts.dto;

import java.time.Instant;

public class AlertRuleResponse {
    
    private String id;
    private String name;
    private Boolean enabled;
    private AlertRuleResponse.SeverityThreshold severityThreshold;
    private String serviceFilter;
    private AlertRuleResponse.Channel channel;
    private String destination;
    private Instant createdAt;
    private Instant updatedAt;
    
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
}
