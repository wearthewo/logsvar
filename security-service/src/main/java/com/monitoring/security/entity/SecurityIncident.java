package com.monitoring.security.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "security_incidents", indexes = {
    @Index(name = "idx_user_timestamp", columnList = "user_id, created_at"),
    @Index(name = "idx_type_timestamp", columnList = "incident_type, created_at"),
    @Index(name = "idx_severity_timestamp", columnList = "severity, created_at"),
    @Index(name = "idx_correlated_anomaly", columnList = "correlated_anomaly_id"),
    @Index(name = "idx_resolved", columnList = "resolved"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
public class SecurityIncident {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String userId;

    @Column(nullable = false)
    private String incidentType;

    @Column(nullable = false)
    private String severity;

    private String route;

    @Column(nullable = false)
    private String ipAddress;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "JSON")
    private String metadata;

    private String correlatedAnomalyId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private Boolean resolved = false;

    public SecurityIncident() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getIncidentType() {
        return incidentType;
    }

    public void setIncidentType(String incidentType) {
        this.incidentType = incidentType;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getRoute() {
        return route;
    }

    public void setRoute(String route) {
        this.route = route;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public String getCorrelatedAnomalyId() {
        return correlatedAnomalyId;
    }

    public void setCorrelatedAnomalyId(String correlatedAnomalyId) {
        this.correlatedAnomalyId = correlatedAnomalyId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Boolean getResolved() {
        return resolved;
    }

    public void setResolved(Boolean resolved) {
        this.resolved = resolved;
    }
}
