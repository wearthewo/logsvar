package com.monitoring.backend.model;

import java.time.Instant;

public record ServiceSummary(
    String name,
    String status,
    
    Instant lastAnomalyAt,
    
    long anomalyCount24h,
    String lastSeverity
) {
    public ServiceSummary {
        if (lastAnomalyAt == null) {
            lastAnomalyAt = Instant.EPOCH;
        }
        if (lastSeverity == null) {
            lastSeverity = "LOW";
        }
    }
}
