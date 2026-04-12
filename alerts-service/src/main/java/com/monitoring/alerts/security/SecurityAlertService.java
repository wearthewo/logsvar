package com.monitoring.alerts.security;

import com.monitoring.alerts.model.AlertRule;
import com.monitoring.alerts.service.AlertProcessingService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class SecurityAlertService {

    private static final Logger logger = LoggerFactory.getLogger(SecurityAlertService.class);
    private static final String SECURITY_EVENTS_TOPIC = "security.events";

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private CircuitBreaker kafkaCircuitBreaker;

    @Autowired
    private Retry kafkaRetry;

    @Autowired
    private AlertProcessingService alertProcessingService;

    // Security event tracking
    private final Map<String, AtomicInteger> userFailedRequestCounts = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> userLastSecurityEvent = new ConcurrentHashMap<>();
    private final AtomicInteger bruteForceAlerts = new AtomicInteger(0);
    private final AtomicInteger unauthorizedAlerts = new AtomicInteger(0);

    @KafkaListener(topics = SECURITY_EVENTS_TOPIC, groupId = "alerts-service-security")
    public void processSecurityEvent(String eventJson) {
        try {
            // Parse security event
            SecurityEvent event = parseSecurityEvent(eventJson);
            
            if (event == null) {
                logger.warn("Failed to parse security event: {}", eventJson);
                return;
            }

            logger.info("Processing security event: {} for user: {}", event.getType(), event.getUserId());

            // Process based on event type
            switch (event.getType()) {
                case "BRUTE_FORCE":
                    handleBruteForceEvent(event);
                    break;
                case "RATE_LIMIT_ABUSE":
                    handleRateLimitAbuseEvent(event);
                    break;
                case "UNAUTHORIZED_ACCESS_ATTEMPT":
                    handleUnauthorizedAccessEvent(event);
                    break;
                case "FORBIDDEN_ACCESS_ATTEMPT":
                    handleForbiddenAccessEvent(event);
                    break;
                case "SENSITIVE_ROUTE_ACCESS_DENIED":
                    handleSensitiveRouteAccessEvent(event);
                    break;
                case "EXCESSIVE_REQUESTS":
                    handleExcessiveRequestsEvent(event);
                    break;
                default:
                    logger.debug("Unhandled security event type: {}", event.getType());
            }

        } catch (Exception e) {
            logger.error("Error processing security event: {}", e.getMessage());
        }
    }

    private void handleBruteForceEvent(SecurityEvent event) {
        String userId = event.getUserId();
        AtomicInteger count = userFailedRequestCounts.computeIfAbsent(userId, k -> new AtomicInteger(0));
        int currentCount = count.incrementAndGet();
        
        userLastSecurityEvent.put(userId, new AtomicLong(System.currentTimeMillis()));

        // Trigger security alert based on failed request count
        if (currentCount >= 5) {
            triggerSecurityAlert(userId, "MULTIPLE_FAILED_REQUESTS", "HIGH", 
                               "User has " + currentCount + " failed requests", event.getMetadata());
        }
        
        if (currentCount >= 10) {
            triggerSecurityAlert(userId, "BRUTE_FORCE_DETECTED", "CRITICAL", 
                               "Brute force attack detected with " + currentCount + " failed requests", 
                               event.getMetadata());
            bruteForceAlerts.incrementAndGet();
        }
    }

    private void handleRateLimitAbuseEvent(SecurityEvent event) {
        String userId = event.getUserId();
        triggerSecurityAlert(userId, "RATE_LIMIT_ABUSE", "MEDIUM", 
                           "User is hitting rate limits", event.getMetadata());
    }

    private void handleUnauthorizedAccessEvent(SecurityEvent event) {
        String userId = event.getUserId();
        triggerSecurityAlert(userId, "UNAUTHORIZED_ACCESS_ATTEMPT", "HIGH", 
                           "Unauthorized access attempt detected", event.getMetadata());
        unauthorizedAlerts.incrementAndGet();
    }

    private void handleForbiddenAccessEvent(SecurityEvent event) {
        String userId = event.getUserId();
        triggerSecurityAlert(userId, "FORBIDDEN_ACCESS_ATTEMPT", "MEDIUM", 
                           "Forbidden access attempt detected", event.getMetadata());
    }

    private void handleSensitiveRouteAccessEvent(SecurityEvent event) {
        String userId = event.getUserId();
        triggerSecurityAlert(userId, "SENSITIVE_ROUTE_ACCESS_DENIED", "HIGH", 
                           "Access denied to sensitive route: " + event.getRoute(), event.getMetadata());
    }

    private void handleExcessiveRequestsEvent(SecurityEvent event) {
        String userId = event.getUserId();
        Map<String, Object> metadata = event.getMetadata();
        
        if (metadata != null && metadata.containsKey("severity")) {
            String severity = (String) metadata.get("severity");
            
            if ("CRITICAL".equals(severity)) {
                triggerSecurityAlert(userId, "EXCESSIVE_REQUESTS", "HIGH", 
                                   "Critical excessive request pattern detected", metadata);
            } else if ("WARNING".equals(severity)) {
                triggerSecurityAlert(userId, "EXCESSIVE_REQUESTS", "MEDIUM", 
                                   "Warning: excessive request pattern detected", metadata);
            }
        }
    }

    private void triggerSecurityAlert(String userId, String alertType, String severity, 
                                   String description, Map<String, Object> metadata) {
        try {
            logger.warn("Triggering security alert: {} for user: {} - {}", alertType, userId, description);

            // Create security alert rule (this would normally be configured in database)
            // For now, we'll create a simple alert and send it through the normal alert processing
            
            // This is a simplified version - in production, you'd have pre-configured security alert rules
            String alertMessage = String.format(
                "Security Alert: %s\nUser: %s\nDescription: %s\nTime: %s\nMetadata: %s",
                alertType, userId, description, LocalDateTime.now(), metadata
            );

            // Log the alert (in production, this would trigger the actual alert system)
            logger.info("SECURITY ALERT: {}", alertMessage);

            // Update metrics
            updateSecurityMetrics(alertType, severity);

        } catch (Exception e) {
            logger.error("Failed to trigger security alert: {}", e.getMessage());
        }
    }

    private SecurityEvent parseSecurityEvent(String eventJson) {
        try {
            // Simple JSON parsing - in production, use proper JSON mapper
            // This is a simplified parser for demonstration
            if (eventJson.contains("\"type\"")) {
                String type = extractJsonValue(eventJson, "type");
                String userId = extractJsonValue(eventJson, "userId");
                String route = extractJsonValue(eventJson, "route");
                String ipAddress = extractJsonValue(eventJson, "ipAddress");
                
                return new SecurityEvent(userId, type, route, ipAddress, null, null);
            }
        } catch (Exception e) {
            logger.error("Error parsing security event: {}", e.getMessage());
        }
        return null;
    }

    private String extractJsonValue(String json, String key) {
        String searchKey = "\"" + key + "\":\"";
        int startIndex = json.indexOf(searchKey);
        if (startIndex != -1) {
            startIndex += searchKey.length();
            int endIndex = json.indexOf("\"", startIndex);
            if (endIndex != -1) {
                return json.substring(startIndex, endIndex);
            }
        }
        return "";
    }

    private void updateSecurityMetrics(String alertType, String severity) {
        // Update security metrics - this would be integrated with Micrometer
        logger.debug("Security metrics updated: type={}, severity={}", alertType, severity);
    }

    // Public methods for API endpoints
    public SecurityStats getSecurityStats() {
        return new SecurityStats(
            bruteForceAlerts.get(),
            unauthorizedAlerts.get(),
            userFailedRequestCounts.size(),
            userLastSecurityEvent.size()
        );
    }

    public List<SecurityAlertSummary> getRecentSecurityAlerts() {
        // This would query the database for recent security alerts
        // For now, return a summary based on current tracking
        return List.of(
            new SecurityAlertSummary("BRUTE_FORCE_DETECTED", bruteForceAlerts.get(), "CRITICAL"),
            new SecurityAlertSummary("UNAUTHORIZED_ACCESS_ATTEMPT", unauthorizedAlerts.get(), "HIGH"),
            new SecurityAlertSummary("MULTIPLE_FAILED_REQUESTS", userFailedRequestCounts.size(), "HIGH")
        );
    }

    // Data classes
    public static class SecurityEvent {
        private final String userId;
        private final String type;
        private final String route;
        private final String ipAddress;
        private final java.time.Instant timestamp;
        private final Map<String, Object> metadata;

        public SecurityEvent(String userId, String type, String route, String ipAddress,
                            java.time.Instant timestamp, Map<String, Object> metadata) {
            this.userId = userId;
            this.type = type;
            this.route = route;
            this.ipAddress = ipAddress;
            this.timestamp = timestamp;
            this.metadata = metadata;
        }

        public String getUserId() { return userId; }
        public String getType() { return type; }
        public String getRoute() { return route; }
        public String getIpAddress() { return ipAddress; }
        public java.time.Instant getTimestamp() { return timestamp; }
        public Map<String, Object> getMetadata() { return metadata; }
    }

    public static class SecurityStats {
        private final int bruteForceAlerts;
        private final int unauthorizedAlerts;
        private final int usersWithFailedRequests;
        private final int activeSecurityEvents;

        public SecurityStats(int bruteForceAlerts, int unauthorizedAlerts, 
                           int usersWithFailedRequests, int activeSecurityEvents) {
            this.bruteForceAlerts = bruteForceAlerts;
            this.unauthorizedAlerts = unauthorizedAlerts;
            this.usersWithFailedRequests = usersWithFailedRequests;
            this.activeSecurityEvents = activeSecurityEvents;
        }

        public int getBruteForceAlerts() { return bruteForceAlerts; }
        public int getUnauthorizedAlerts() { return unauthorizedAlerts; }
        public int getUsersWithFailedRequests() { return usersWithFailedRequests; }
        public int getActiveSecurityEvents() { return activeSecurityEvents; }
    }

    public static class SecurityAlertSummary {
        private final String alertType;
        private final int count;
        private final String severity;

        public SecurityAlertSummary(String alertType, int count, String severity) {
            this.alertType = alertType;
            this.count = count;
            this.severity = severity;
        }

        public String getAlertType() { return alertType; }
        public int getCount() { return count; }
        public String getSeverity() { return severity; }
    }
}
