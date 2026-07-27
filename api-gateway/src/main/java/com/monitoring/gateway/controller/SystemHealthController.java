package com.monitoring.gateway.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.core.ParameterizedTypeReference;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/system")
public class SystemHealthController {
    private final WebClient webClient;
    private final String monitoringUrl;
    private final String alertsUrl;
    private final String securityUrl;
    private final String aiUrl;

    public SystemHealthController(WebClient.Builder builder,
            @Value("${services.monitoring-url:http://monitoring-backend:8080}") String monitoringUrl,
            @Value("${services.alerts-url:http://alerts-service:8081}") String alertsUrl,
            @Value("${services.security-url:http://security-service:8083}") String securityUrl,
            @Value("${services.ai-url:http://ai-agent:8001}") String aiUrl) {
        this.webClient = builder.build();
        this.monitoringUrl = monitoringUrl;
        this.alertsUrl = alertsUrl;
        this.securityUrl = securityUrl;
        this.aiUrl = aiUrl;
    }

    @GetMapping("/health")
    public Mono<ResponseEntity<Map<String, Object>>> health() {
        return Mono.zip(
            check("monitoring-backend", monitoringUrl + "/actuator/health"),
            check("alerts-service", alertsUrl + "/actuator/health"),
            check("security-service", securityUrl + "/actuator/health"),
            check("ai-agent", aiUrl + "/health")
        ).map(tuple -> {
            Map<String, Object> services = new LinkedHashMap<>();
            services.put("monitoring-backend", tuple.getT1());
            services.put("alerts-service", tuple.getT2());
            services.put("security-service", tuple.getT3());
            services.put("ai-agent", tuple.getT4());
            boolean allUp = services.values().stream()
                    .map(value -> (Map<?, ?>) value)
                    .allMatch(value -> "UP".equals(value.get("status")));
            return ResponseEntity.ok(Map.of(
                    "status", allUp ? "UP" : "DEGRADED",
                    "timestamp", Instant.now().toString(),
                    "services", services));
        });
    }

    private Mono<Map<String, Object>> check(String name, String url) {
        return webClient.get().uri(url).retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .map(body -> {
                    String raw = String.valueOf(body.getOrDefault("status", "UP"));
                    String status = raw.equalsIgnoreCase("error") || raw.equalsIgnoreCase("down") ? "DOWN" :
                            (raw.equalsIgnoreCase("degraded") ? "DEGRADED" : "UP");
                    return Map.<String, Object>of("status", status, "details", body);
                })
                .timeout(Duration.ofSeconds(3))
                .onErrorReturn(Map.<String, Object>of("status", "DOWN", "message", name + " unavailable"));
    }
}
