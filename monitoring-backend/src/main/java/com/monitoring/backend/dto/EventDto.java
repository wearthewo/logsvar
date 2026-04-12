package com.monitoring.backend.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "eventType"
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = HttpRequestEventDto.class, name = "HTTP_REQUEST"),
    @JsonSubTypes.Type(value = ExceptionEventDto.class, name = "EXCEPTION"),
    @JsonSubTypes.Type(value = SystemMetricEventDto.class, name = "SYSTEM_METRIC"),
    @JsonSubTypes.Type(value = DbQueryEventDto.class, name = "DB_QUERY")
})
public abstract class EventDto {
    
    @NotBlank(message = "Service name is required")
    private String serviceName;
    
    @NotNull(message = "Event type is required")
    private EventType eventType;
    
    @NotNull(message = "Timestamp is required")
    private Instant timestamp;
    
    public EventDto() {
        this.id = UUID.randomUUID().toString();
        this.timestamp = Instant.now();
    }
    
    private final String id;
    
    public String getId() {
        return id;
    }
    
    public String getServiceName() {
        return serviceName;
    }
    
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }
    
    public EventType getEventType() {
        return eventType;
    }
    
    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }
    
    public Instant getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
    
    public enum EventType {
        HTTP_REQUEST,
        EXCEPTION,
        SYSTEM_METRIC,
        DB_QUERY
    }
}
