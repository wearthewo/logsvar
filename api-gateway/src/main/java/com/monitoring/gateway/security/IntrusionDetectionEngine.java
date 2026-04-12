package com.monitoring.gateway.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Map;

@Component
public class IntrusionDetectionEngine {

    private static final Logger logger = LoggerFactory.getLogger(IntrusionDetectionEngine.class);

    // Sliding window configurations
    private static final int BASELINE_REQUESTS_PER_MINUTE = 100;
    private static final int BRUTE_FORCE_THRESHOLD = 10;
    private static final int WARN_THRESHOLD_PERCENTAGE = 150; // 150% of baseline
    private static final int CRITICAL_THRESHOLD_PERCENTAGE = 300; // 300% of baseline

    // Per-user tracking data structures
    private final ConcurrentHashMap<String, UserRequestTracker> userTrackers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, FailedRequestTracker> failedRequestTrackers = new ConcurrentHashMap<>();

    public IntrusionResult checkForIntrusion(String userId, String route, String ipAddress, int statusCode) {
        IntrusionResult result = new IntrusionResult(false, null, Map.of());

        try {
            // Check for brute force attempts
            IntrusionResult bruteForceResult = checkBruteForce(userId, route, ipAddress, statusCode);
            if (bruteForceResult.isIntrusionDetected()) {
                return bruteForceResult;
            }

            // Check for rate limit abuse
            IntrusionResult rateLimitResult = checkRateLimitAbuse(userId, route, ipAddress, statusCode);
            if (rateLimitResult.isIntrusionDetected()) {
                return rateLimitResult;
            }

            // Check for excessive request patterns
            IntrusionResult excessiveRequestResult = checkExcessiveRequests(userId, route, ipAddress, statusCode);
            if (excessiveRequestResult.isIntrusionDetected()) {
                return excessiveRequestResult;
            }

        } catch (Exception e) {
            logger.error("Error in intrusion detection for user {}: {}", userId, e.getMessage());
        }

        return result;
    }

    private IntrusionResult checkBruteForce(String userId, String route, String ipAddress, int statusCode) {
        // Track failed requests per user
        FailedRequestTracker failedTracker = failedRequestTrackers.computeIfAbsent(userId, 
            k -> new FailedRequestTracker());

        if (statusCode >= 400) {
            failedTracker.incrementFailedRequest();
        }

        // Check brute force threshold
        int failedCount = failedTracker.getFailedRequestCount();
        if (failedCount >= BRUTE_FORCE_THRESHOLD) {
            logger.warn("Brute force detected for user {}: {} failed requests", userId, failedCount);
            
            Map<String, Object> metadata = Map.of(
                "failedRequestCount", failedCount,
                "threshold", BRUTE_FORCE_THRESHOLD,
                "timeWindow", "1 minute"
            );

            return new IntrusionResult(true, "BRUTE_FORCE", metadata);
        }

        // Reset counter after successful requests
        if (statusCode < 400) {
            failedTracker.reset();
        }

        return new IntrusionResult(false, null, Map.of());
    }

    private IntrusionResult checkRateLimitAbuse(String userId, String route, String ipAddress, int statusCode) {
        if (statusCode == 429) {
            // Track rate limit violations
            UserRequestTracker tracker = userTrackers.computeIfAbsent(userId, 
                k -> new UserRequestTracker());

            tracker.incrementRateLimitViolation();

            int violations = tracker.getRateLimitViolations();
            if (violations >= 3) { // 3 rate limit violations in short time
                logger.warn("Rate limit abuse detected for user {}: {} violations", userId, violations);
                
                Map<String, Object> metadata = Map.of(
                    "rateLimitViolations", violations,
                    "route", route,
                    "timeWindow", "5 minutes"
                );

                return new IntrusionResult(true, "RATE_LIMIT_ABUSE", metadata);
            }
        }

        return new IntrusionResult(false, null, Map.of());
    }

    private IntrusionResult checkExcessiveRequests(String userId, String route, String ipAddress, int statusCode) {
        UserRequestTracker tracker = userTrackers.computeIfAbsent(userId, 
            k -> new UserRequestTracker());

        // Increment request count
        tracker.incrementRequest();

        // Get current request rate
        int requestsPerMinute = tracker.getRequestsPerMinute();
        
        if (requestsPerMinute > BASELINE_REQUESTS_PER_MINUTE) {
            int percentage = (requestsPerMinute * 100) / BASELINE_REQUESTS_PER_MINUTE;
            
            if (percentage >= CRITICAL_THRESHOLD_PERCENTAGE) {
                logger.warn("Critical excessive requests detected for user {}: {} requests/minute ({}% of baseline)", 
                          userId, requestsPerMinute, percentage);
                
                Map<String, Object> metadata = Map.of(
                    "requestsPerMinute", requestsPerMinute,
                    "baseline", BASELINE_REQUESTS_PER_MINUTE,
                    "percentage", percentage,
                    "severity", "CRITICAL"
                );

                return new IntrusionResult(true, "EXCESSIVE_REQUESTS", metadata);
            } else if (percentage >= WARN_THRESHOLD_PERCENTAGE) {
                logger.info("Warning: excessive requests detected for user {}: {} requests/minute ({}% of baseline)", 
                          userId, requestsPerMinute, percentage);
                
                Map<String, Object> metadata = Map.of(
                    "requestsPerMinute", requestsPerMinute,
                    "baseline", BASELINE_REQUESTS_PER_MINUTE,
                    "percentage", percentage,
                    "severity", "WARNING"
                );

                return new IntrusionResult(true, "EXCESSIVE_REQUESTS", metadata);
            }
        }

        return new IntrusionResult(false, null, Map.of());
    }

    // Cleanup old data to prevent memory leaks
    public void cleanup() {
        Instant now = Instant.now();
        
        // Clean up old user trackers
        userTrackers.entrySet().removeIf(entry -> {
            UserRequestTracker tracker = entry.getValue();
            return tracker.getLastAccess().isBefore(now.minus(10, ChronoUnit.MINUTES));
        });
        
        // Clean up old failed request trackers
        failedRequestTrackers.entrySet().removeIf(entry -> {
            FailedRequestTracker tracker = entry.getValue();
            return tracker.getLastAccess().isBefore(now.minus(5, ChronoUnit.MINUTES));
        });
    }

    // Inner classes for tracking
    private static class UserRequestTracker {
        private final AtomicInteger requestCount = new AtomicInteger(0);
        private final AtomicInteger rateLimitViolations = new AtomicInteger(0);
        private final AtomicLong lastRequestTime = new AtomicLong(System.currentTimeMillis());
        private volatile Instant lastAccess = Instant.now();

        public void incrementRequest() {
            requestCount.incrementAndGet();
            lastRequestTime.set(System.currentTimeMillis());
            lastAccess = Instant.now();
        }

        public void incrementRateLimitViolation() {
            rateLimitViolations.incrementAndGet();
            lastAccess = Instant.now();
        }

        public int getRequestsPerMinute() {
            long now = System.currentTimeMillis();
            long lastRequest = lastRequestTime.get();
            
            // If last request was more than a minute ago, return 0
            if (now - lastRequest > 60000) {
                return 0;
            }
            
            return requestCount.get();
        }

        public int getRateLimitViolations() {
            return rateLimitViolations.get();
        }

        public Instant getLastAccess() {
            return lastAccess;
        }
    }

    private static class FailedRequestTracker {
        private final AtomicInteger failedRequests = new AtomicInteger(0);
        private final AtomicLong lastFailureTime = new AtomicLong(System.currentTimeMillis());
        private volatile Instant lastAccess = Instant.now();

        public void incrementFailedRequest() {
            failedRequests.incrementAndGet();
            lastFailureTime.set(System.currentTimeMillis());
            lastAccess = Instant.now();
        }

        public int getFailedRequestCount() {
            long now = System.currentTimeMillis();
            long lastFailure = lastFailureTime.get();
            
            // Reset if last failure was more than a minute ago
            if (now - lastFailure > 60000) {
                reset();
                return 0;
            }
            
            return failedRequests.get();
        }

        public void reset() {
            failedRequests.set(0);
            lastAccess = Instant.now();
        }

        public Instant getLastAccess() {
            return lastAccess;
        }
    }

    public static class IntrusionResult {
        private final boolean intrusionDetected;
        private final String intrusionType;
        private final Map<String, Object> metadata;

        public IntrusionResult(boolean intrusionDetected, String intrusionType, Map<String, Object> metadata) {
            this.intrusionDetected = intrusionDetected;
            this.intrusionType = intrusionType;
            this.metadata = metadata;
        }

        public boolean isIntrusionDetected() {
            return intrusionDetected;
        }

        public String getIntrusionType() {
            return intrusionType;
        }

        public Map<String, Object> getMetadata() {
            return metadata;
        }
    }
}
