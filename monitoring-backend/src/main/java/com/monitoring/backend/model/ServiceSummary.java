package com.monitoring.backend.model;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;

public record ServiceSummary(
    String name,
    String status,
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
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
