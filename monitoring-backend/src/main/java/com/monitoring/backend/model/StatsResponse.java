package com.monitoring.backend.model;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;
import java.util.Map;

public record StatsResponse(
    long total,
    Map<String, Long> bySeverity,
    Map<String, Long> byService,
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    Instant since
) {
    public StatsResponse {
        if (since == null) {
            since = Instant.now();
        }
    }
}
