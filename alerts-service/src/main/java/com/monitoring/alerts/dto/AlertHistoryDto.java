package com.monitoring.alerts.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record AlertHistoryDto(
    @NotBlank String id,
    @NotBlank String anomalyId,
    @NotBlank String ruleId,
    @NotBlank String channel,
    String destination,
    @NotBlank String status,
    String errorMessage,
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    @NotNull Instant sentAt
) {
    public AlertHistoryDto {
        if (sentAt == null) {
            sentAt = Instant.now();
        }
    }
}
