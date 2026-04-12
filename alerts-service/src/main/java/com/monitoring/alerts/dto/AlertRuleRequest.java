package com.monitoring.alerts.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class AlertRuleRequest {
    
    @NotBlank(message = "Name is required")
    private String name;
    
    private Boolean enabled = true;
    
    @NotNull(message = "Severity threshold is required")
    private AlertRuleRequest.SeverityThreshold severityThreshold;
    
    private String serviceFilter;
    
    @NotNull(message = "Channel is required")
    private AlertRuleRequest.Channel channel;
    
    private String destination;
    
    public enum SeverityThreshold {
        LOW, MEDIUM, HIGH, CRITICAL
    }
    
    public enum Channel {
        EMAIL, SLACK, WEBHOOK, IN_APP
    }
    
    // Getters and Setters
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
}
