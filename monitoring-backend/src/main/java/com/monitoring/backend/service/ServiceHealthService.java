package com.monitoring.backend.service;

import com.monitoring.backend.entity.Anomaly;
import com.monitoring.backend.model.ServiceSummary;
import com.monitoring.backend.repository.AnomalyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ServiceHealthService {
    
    private static final Logger logger = LoggerFactory.getLogger(ServiceHealthService.class);
    
    @Autowired
    private AnomalyRepository anomalyRepository;
    
    public List<ServiceSummary> getServiceSummaries() {
        long startTime = System.currentTimeMillis();
        logger.debug("Fetching service health summaries");
        
        try {
            Instant now = Instant.now();
            Instant last30Minutes = now.minus(java.time.Duration.ofMinutes(30));
            Instant last24Hours = now.minus(java.time.Duration.ofHours(24));
            
            // Get all anomalies in the last 24 hours using optimized query
            List<Anomaly> recentAnomalies = anomalyRepository.findRecentAll(last24Hours);
            
            // Group by service name
            Map<String, List<Anomaly>> anomaliesByService = recentAnomalies.stream()
                .collect(Collectors.groupingBy(Anomaly::getServiceName));
            
            List<ServiceSummary> summaries = new ArrayList<>();
            
            for (Map.Entry<String, List<Anomaly>> entry : anomaliesByService.entrySet()) {
                String serviceName = entry.getKey();
                List<Anomaly> serviceAnomalies = entry.getValue();
                
                // Get anomalies in last 30 minutes for health status
                List<Anomaly> recent30MinAnomalies = serviceAnomalies.stream()
                    .filter(a -> a.getDetectedAt().isAfter(last30Minutes))
                    .collect(Collectors.toList());
                
                // Determine health status
                String status = calculateHealthStatus(recent30MinAnomalies);
                
                // Get last anomaly
                Anomaly lastAnomaly = serviceAnomalies.isEmpty() ? null : serviceAnomalies.get(0);
                Instant lastAnomalyAt = lastAnomaly != null ? lastAnomaly.getDetectedAt() : null;
                String lastSeverity = lastAnomaly != null ? lastAnomaly.getSeverity().name() : null;
                
                // Count anomalies in last 24 hours
                long anomalyCount24h = serviceAnomalies.size();
                
                summaries.add(new ServiceSummary(
                    serviceName,
                    status,
                    lastAnomalyAt,
                    anomalyCount24h,
                    lastSeverity
                ));
            }
            
            // Sort by service name
            summaries.sort(Comparator.comparing(ServiceSummary::name));
            
            long queryTime = System.currentTimeMillis() - startTime;
            logger.info("Service health query completed in {}ms - returned {} services", 
                       queryTime, summaries.size());
            
            return summaries;
        } catch (Exception e) {
            long queryTime = System.currentTimeMillis() - startTime;
            logger.error("Service health query failed after {}ms: {}", queryTime, e.getMessage());
            throw new RuntimeException("Failed to fetch service summaries", e);
        }
    }
    
    private String calculateHealthStatus(List<Anomaly> recentAnomalies) {
        if (recentAnomalies.isEmpty()) {
            return "HEALTHY";
        }
        
        // Check for CRITICAL anomalies
        boolean hasCritical = recentAnomalies.stream()
            .anyMatch(a -> a.getSeverity() == Anomaly.Severity.CRITICAL);
        if (hasCritical) {
            return "CRITICAL";
        }
        
        // Check for HIGH anomalies
        boolean hasHigh = recentAnomalies.stream()
            .anyMatch(a -> a.getSeverity() == Anomaly.Severity.HIGH);
        if (hasHigh) {
            return "DEGRADED";
        }
        
        // Check for LOW or MEDIUM anomalies
        boolean hasLowOrMedium = recentAnomalies.stream()
            .anyMatch(a -> a.getSeverity() == Anomaly.Severity.LOW || a.getSeverity() == Anomaly.Severity.MEDIUM);
        if (hasLowOrMedium) {
            return "WARNING";
        }
        
        return "HEALTHY";
    }
}
