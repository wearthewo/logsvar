package com.monitoring.backend.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "monitoring_events")
public class MonitoringEvent {
    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "service_name", nullable = false, length = 128)
    private String serviceName;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private EventType eventType;

    @Column(name = "payload", nullable = false, columnDefinition = "JSON")
    private String payload;

    @CreationTimestamp
    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    public enum EventType {
        HTTP_REQUEST, EXCEPTION, SYSTEM_METRIC, DB_QUERY
    }

    // Constructors
    public MonitoringEvent() {}

    public MonitoringEvent(String id, String serviceName, EventType eventType, String payload) {
        this.id = id;
        this.serviceName = serviceName;
        this.eventType = eventType;
        this.payload = payload;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }

    public EventType getEventType() { return eventType; }
    public void setEventType(EventType eventType) { this.eventType = eventType; }

    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }

    public Instant getReceivedAt() { return receivedAt; }
    public void setReceivedAt(Instant receivedAt) { this.receivedAt = receivedAt; }
}
