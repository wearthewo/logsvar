package com.monitoring.alerts.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public class AnomalyDto {
    
    private String id;
    private String eventId;
    private String serviceName;
    private String severity;
    private String reason;
    @JsonProperty("recommendedAction")
    private String recommendedAction;
    @JsonProperty("detectedAt")
    private Instant detectedAt;
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getEventId() {
        return eventId;
    }
    
    public void setEventId(String eventId) {
        this.eventId = eventId;
    }
    
    public String getServiceName() {
        return serviceName;
    }
    
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }
    
    public String getSeverity() {
        return severity;
    }
    
    public void setSeverity(String severity) {
        this.severity = severity;
    }
    
    public String getReason() {
        return reason;
    }
    
    public void setReason(String reason) {
        this.reason = reason;
    }
    
    public String getRecommendedAction() {
        return recommendedAction;
    }
    
    public void setRecommendedAction(String recommendedAction) {
        this.recommendedAction = recommendedAction;
    }
    
    public Instant getDetectedAt() {
        return detectedAt;
    }
    
    public void setDetectedAt(Instant detectedAt) {
        this.detectedAt = detectedAt;
    }
}
