package com.monitoring.alerts.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record AlertRuleDto(
    String id,
    @NotBlank String name,
    @NotNull Boolean enabled,
    @NotBlank String severityThreshold,
    String serviceFilter,
    @NotBlank String channel,
    String destination,
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    Instant createdAt,
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    Instant updatedAt
) {
    public AlertRuleDto {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (updatedAt == null) {
            updatedAt = Instant.now();
        }
    }
}
