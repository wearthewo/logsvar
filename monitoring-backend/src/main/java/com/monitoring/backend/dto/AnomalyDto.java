package com.monitoring.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record AnomalyDto(
    @NotBlank String id,
    @NotBlank String eventId,
    @NotBlank String serviceName,
    @NotBlank String severity,
    @NotBlank String reason,
    String recommendedAction,
    
    @NotNull Instant detectedAt
) {
    public AnomalyDto {
        if (detectedAt == null) {
            detectedAt = Instant.now();
        }
    }
}
