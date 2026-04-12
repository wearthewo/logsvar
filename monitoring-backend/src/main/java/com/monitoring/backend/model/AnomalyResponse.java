package com.monitoring.backend.model;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;

public record AnomalyResponse(
    String id,
    String eventId,
    String serviceName,
    String severity,
    String reason,
    String recommendedAction,
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    Instant detectedAt
) {
    public AnomalyResponse {
        if (detectedAt == null) {
            detectedAt = Instant.now();
        }
    }
}
