package com.monitoring.backend.model;

import java.time.Instant;
import java.util.Map;

public record StatsResponse(
    long total,
    Map<String, Long> bySeverity,
    Map<String, Long> byService,
    
    Instant since
) {
    public StatsResponse {
        if (since == null) {
            since = Instant.now();
        }
    }
}
