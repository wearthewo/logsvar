package com.monitoring.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Map;

public record MonitoringEventDto(
    @NotBlank String id,
    @NotBlank String serviceName,
    @NotBlank String eventType,
    @NotNull Map<String, Object> payload,
    
    Instant timestamp
) {
    public MonitoringEventDto {
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }
}
