package com.monitoring.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Map;

public record EventRequest(
    String id,
    @NotBlank(message = "Service name is required") String serviceName,
    @NotNull(message = "Event type is required") EventDto.EventType eventType,
    Instant timestamp,
    @NotNull(message = "Payload is required") Map<String, Object> payload
) {}
