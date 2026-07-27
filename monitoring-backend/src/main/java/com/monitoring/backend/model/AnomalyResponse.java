package com.monitoring.backend.model;

import java.time.Instant;

public record AnomalyResponse(
    String id,
    String eventId,
    String serviceName,
    String severity,
    String reason,
    String recommendedAction,
    
    Instant detectedAt
) {
    public AnomalyResponse {
        if (detectedAt == null) {
            detectedAt = Instant.now();
        }
    }
}
