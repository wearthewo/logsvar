package com.monitoring.backend.dto;

import java.time.Instant;
import java.util.Map;

public record EventEnvelope(
    String id,
    String serviceName,
    EventDto.EventType eventType,
    Instant timestamp,
    Map<String, Object> payload
) {}
