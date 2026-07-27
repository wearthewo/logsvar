package com.monitoring.gateway.filter;

import com.monitoring.gateway.security.IntrusionDetectionEngine;
import com.monitoring.gateway.security.SecurityEventPublisher;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class SecurityAuditFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(SecurityAuditFilter.class);
    private static final String USER_ID_HEADER = "X-User-ID";
    private static final String SECURITY_EVENT_TOPIC = "security.events";

    @Autowired
    private MeterRegistry meterRegistry;

    @Autowired
    private SecurityEventPublisher securityEventPublisher;

    @Autowired
    private IntrusionDetectionEngine intrusionDetection;

    // Security metrics
    private final Counter securityEventsCounter;
    private final Counter failedAuthCounter;
    private final Counter tokenRejectionCounter;
    private final Counter intrusionAlertsCounter;

    public SecurityAuditFilter(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        // Initialize security metrics
        this.securityEventsCounter = Counter.builder("security_events_total")
                .description("Total number of security events")
                .register(meterRegistry);
        
        this.failedAuthCounter = Counter.builder("failed_auth_total")
                .description("Total number of failed authentication attempts")
                .register(meterRegistry);
        
        this.tokenRejectionCounter = Counter.builder("token_rejection_total")
                .description("Total number of rejected tokens")
                .register(meterRegistry);
        
        this.intrusionAlertsCounter = Counter.builder("intrusion_alerts_total")
                .description("Total number of intrusion alerts")
                .register(meterRegistry);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long startTime = System.currentTimeMillis();
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();

        // Extract user information from JWT (already validated by OAuth2 filter)
        String ipAddress = getClientIpAddress(request);
        String route = request.getPath().value();
        String method = request.getMethod().name();

        // Log request details (without sensitive data)
        return exchange.getPrincipal()
          .map(principal -> principal instanceof JwtAuthenticationToken jwt
                  ? Optional.ofNullable(jwt.getToken().getClaimAsString("preferred_username")).orElse(jwt.getName())
                  : principal.getName())
          .defaultIfEmpty("anonymous")
          .flatMap(userId -> {
            logger.info("Security audit: userId={}, route={}, method={}, ip={}", userId, route, method, ipAddress);
            return chain.filter(exchange).doFinally(signalType -> {
            long endTime = System.currentTimeMillis();
            long latencyMs = endTime - startTime;
            int statusCode = response.getStatusCode() != null ? response.getStatusCode().value() : 0;

            // Record security audit
            recordSecurityAudit(userId, route, method, statusCode, latencyMs, ipAddress);

            // Check for security events
            checkSecurityEvents(userId, route, method, statusCode, ipAddress, latencyMs);

            // Update metrics
            updateSecurityMetrics(route, statusCode, userId);
            });
          });
    }

    private String getClientIpAddress(ServerHttpRequest request) {
        String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeaders().getFirst("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddress() != null ? 
               request.getRemoteAddress().getAddress().getHostAddress() : "unknown";
    }

    private void recordSecurityAudit(String userId, String route, String method, 
                                    int statusCode, long latencyMs, String ipAddress) {
        try {
            SecurityAuditEvent auditEvent = new SecurityAuditEvent(
                userId,
                "API_ACCESS",
                route,
                statusCode >= 400 ? "FAILED" : "SUCCESS",
                ipAddress,
                route,
                method,
                statusCode,
                latencyMs,
                LocalDateTime.now()
            );

            // Publish to security audit service
            securityEventPublisher.publishAuditEvent(auditEvent);
            
        } catch (Exception e) {
            logger.error("Failed to record security audit", e);
        }
    }

    private void checkSecurityEvents(String userId, String route, String method, 
                                   int statusCode, String ipAddress, long latencyMs) {
        // Check for failed authentication
        if (statusCode == 401) {
            failedAuthCounter.increment();
            publishSecurityEvent(userId, "UNAUTHORIZED_ACCESS_ATTEMPT", route, ipAddress, 
                              Map.of("method", method, "statusCode", statusCode));
        }

        // Check for forbidden access
        if (statusCode == 403) {
            publishSecurityEvent(userId, "FORBIDDEN_ACCESS_ATTEMPT", route, ipAddress, 
                              Map.of("method", method, "statusCode", statusCode));
        }

        // Check for rate limit abuse (429)
        if (statusCode == 429) {
            publishSecurityEvent(userId, "RATE_LIMIT_ABUSE", route, ipAddress, 
                              Map.of("method", method, "statusCode", statusCode));
        }

        // Check for access to sensitive routes
        if (isSensitiveRoute(route)) {
            if (statusCode >= 400) {
                publishSecurityEvent(userId, "SENSITIVE_ROUTE_ACCESS_DENIED", route, ipAddress, 
                                  Map.of("method", method, "statusCode", statusCode));
            }
        }

        // Check intrusion detection patterns
        IntrusionDetectionEngine.IntrusionResult result = 
            intrusionDetection.checkForIntrusion(userId, route, ipAddress, statusCode);

        if (result.isIntrusionDetected()) {
            intrusionAlertsCounter.increment();
            publishSecurityEvent(userId, result.getIntrusionType(), route, ipAddress, 
                              result.getMetadata());
        }
    }

    private boolean isSensitiveRoute(String route) {
        return route.startsWith("/ai-agent") || 
               route.startsWith("/kafka") || 
               route.startsWith("/mysql") || 
               route.startsWith("/redis") ||
               route.startsWith("/admin") ||
               route.startsWith("/actuator");
    }

    private void publishSecurityEvent(String userId, String eventType, String route, 
                                    String ipAddress, Map<String, Object> metadata) {
        try {
            SecurityEvent securityEvent = new SecurityEvent(
                userId,
                eventType,
                route,
                ipAddress,
                Instant.now(),
                metadata
            );

            securityEventPublisher.publishSecurityEvent(securityEvent);
            securityEventsCounter.increment();
            
        } catch (Exception e) {
            logger.error("Failed to publish security event", e);
        }
    }

    private void updateSecurityMetrics(String route, int statusCode, String userId) {
        // Update various security metrics based on request patterns
        if (statusCode == 401) {
            tokenRejectionCounter.increment();
        }
    }

    @Override
    public int getOrder() {
        // Run after OAuth2 validation but before routing
        return 5;
    }

    // Security event data classes
    public static class SecurityAuditEvent {
        private final String userId;
        private final String action;
        private final String resource;
        private final String status;
        private final String ipAddress;
        private final String route;
        private final String method;
        private final int statusCode;
        private final long latencyMs;
        private final LocalDateTime timestamp;

        public SecurityAuditEvent(String userId, String action, String resource, String status,
                                 String ipAddress, String route, String method, int statusCode,
                                 long latencyMs, LocalDateTime timestamp) {
            this.userId = userId;
            this.action = action;
            this.resource = resource;
            this.status = status;
            this.ipAddress = ipAddress;
            this.route = route;
            this.method = method;
            this.statusCode = statusCode;
            this.latencyMs = latencyMs;
            this.timestamp = timestamp;
        }

        // Getters
        public String getUserId() { return userId; }
        public String getAction() { return action; }
        public String getResource() { return resource; }
        public String getStatus() { return status; }
        public String getIpAddress() { return ipAddress; }
        public String getRoute() { return route; }
        public String getMethod() { return method; }
        public int getStatusCode() { return statusCode; }
        public long getLatencyMs() { return latencyMs; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }

    public static class SecurityEvent {
        private final String userId;
        private final String type;
        private final String route;
        private final String ipAddress;
        private final Instant timestamp;
        private final Map<String, Object> metadata;

        public SecurityEvent(String userId, String type, String route, String ipAddress,
                            Instant timestamp, Map<String, Object> metadata) {
            this.userId = userId;
            this.type = type;
            this.route = route;
            this.ipAddress = ipAddress;
            this.timestamp = timestamp;
            this.metadata = metadata;
        }

        // Getters
        public String getUserId() { return userId; }
        public String getType() { return type; }
        public String getRoute() { return route; }
        public String getIpAddress() { return ipAddress; }
        public Instant getTimestamp() { return timestamp; }
        public Map<String, Object> getMetadata() { return metadata; }
    }
}
