package com.monitoring.backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Map;

public record MonitoringEventDto(
    @NotBlank String id,
    @NotBlank String serviceName,
    @NotBlank String eventType,
    @NotNull Map<String, Object> payload,
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    Instant timestamp
) {
    public MonitoringEventDto {
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }
}
