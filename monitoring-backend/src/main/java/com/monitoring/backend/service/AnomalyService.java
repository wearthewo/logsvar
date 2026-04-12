package com.monitoring.backend.service;

import com.monitoring.backend.model.AnomalyResponse;
import com.monitoring.backend.model.PageResponse;
import com.monitoring.backend.model.StatsResponse;
import com.monitoring.backend.entity.Anomaly;
import com.monitoring.backend.repository.AnomalyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AnomalyService {
    
    private static final Logger logger = LoggerFactory.getLogger(AnomalyService.class);
    
    private final AnomalyRepository anomalyRepository;
    
    public AnomalyService(AnomalyRepository anomalyRepository) {
        this.anomalyRepository = anomalyRepository;
    }
    
    public PageResponse<AnomalyResponse> getAllAnomalies(Pageable pageable) {
        long startTime = System.currentTimeMillis();
        logger.debug("Fetching all anomalies with pagination: page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());
        
        try {
            Page<Anomaly> anomalies = anomalyRepository.findAllNewestFirst(pageable);
            
            List<AnomalyResponse> content = anomalies.getContent().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
            
            PageResponse<AnomalyResponse> response = new PageResponse<>(
                content,
                anomalies.getTotalElements(),
                anomalies.getTotalPages(),
                anomalies.getNumber(),
                anomalies.getSize()
            );
            
            long queryTime = System.currentTimeMillis() - startTime;
            logger.info("Query completed in {}ms - returned {} of {} anomalies", 
                       queryTime, content.size(), anomalies.getTotalElements());
            
            return response;
        } catch (Exception e) {
            long queryTime = System.currentTimeMillis() - startTime;
            logger.error("Query failed after {}ms: {}", queryTime, e.getMessage());
            throw new RuntimeException("Failed to fetch anomalies", e);
        }
    }
    
    public PageResponse<AnomalyResponse> getAnomaliesWithFilters(String serviceName, 
                                                             String severity, 
                                                             Instant from, 
                                                             Instant to, 
                                                             Pageable pageable) {
        long startTime = System.currentTimeMillis();
        logger.debug("Fetching anomalies with filters - service: {}, severity: {}, from: {}, to: {}, page: {}, size: {}", 
                    serviceName, severity, from, to, pageable.getPageNumber(), pageable.getPageSize());
        
        try {
            Anomaly.Severity severityEnum = null;
            if (severity != null && !severity.trim().isEmpty()) {
                severityEnum = Anomaly.Severity.valueOf(severity.toUpperCase());
            }
            
            Page<Anomaly> anomalies = anomalyRepository.findByFiltersNewestFirst(
                serviceName, severityEnum, from, to, pageable);
            
            List<AnomalyResponse> content = anomalies.getContent().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
            
            PageResponse<AnomalyResponse> response = new PageResponse<>(
                content,
                anomalies.getTotalElements(),
                anomalies.getTotalPages(),
                anomalies.getNumber(),
                anomalies.getSize()
            );
            
            long queryTime = System.currentTimeMillis() - startTime;
            logger.info("Filter query completed in {}ms - returned {} of {} anomalies", 
                       queryTime, content.size(), anomalies.getTotalElements());
            
            return response;
        } catch (IllegalArgumentException e) {
            long queryTime = System.currentTimeMillis() - startTime;
            logger.error("Invalid parameter after {}ms: {}", queryTime, e.getMessage());
            throw e;
        } catch (Exception e) {
            long queryTime = System.currentTimeMillis() - startTime;
            logger.error("Filter query failed after {}ms: {}", queryTime, e.getMessage());
            throw new RuntimeException("Failed to fetch filtered anomalies", e);
        }
    }
    
    public AnomalyResponse getAnomalyById(String id) {
        long startTime = System.currentTimeMillis();
        logger.debug("Fetching anomaly by id: {}", id);
        
        try {
            Anomaly anomaly = anomalyRepository.findByIdAnomaly(id);
            if (anomaly == null) {
                long queryTime = System.currentTimeMillis() - startTime;
                logger.info("Anomaly not found after {}ms for id: {}", queryTime, id);
                return null;
            }
            
            AnomalyResponse response = convertToResponse(anomaly);
            long queryTime = System.currentTimeMillis() - startTime;
            logger.info("Anomaly lookup completed in {}ms for id: {}", queryTime, id);
            
            return response;
        } catch (Exception e) {
            long queryTime = System.currentTimeMillis() - startTime;
            logger.error("Anomaly lookup failed after {}ms for id {}: {}", queryTime, id, e.getMessage());
            throw new RuntimeException("Failed to fetch anomaly", e);
        }
    }
    
    public StatsResponse getAnomalyStatistics(Instant since) {
        long startTime = System.currentTimeMillis();
        
        if (since == null) {
            since = Instant.now().minus(java.time.Duration.ofHours(24)); // Default to last 24 hours
        }
        
        logger.debug("Fetching anomaly statistics since: {}", since);
        
        try {
            long totalCount = anomalyRepository.countSince(since);
            List<Object[]> severityCounts = anomalyRepository.countBySeveritySince(since);
            List<Object[]> serviceCounts = anomalyRepository.countByServiceSince(since);
            
            Map<String, Long> severityBreakdown = severityCounts.stream()
                .collect(Collectors.toMap(
                    result -> ((Anomaly.Severity) result[0]).name(),
                    result -> (Long) result[1]
                ));
            
            Map<String, Long> serviceBreakdown = serviceCounts.stream()
                .collect(Collectors.toMap(
                    result -> (String) result[0],
                    result -> (Long) result[1]
                ));
            
            StatsResponse response = new StatsResponse(totalCount, severityBreakdown, serviceBreakdown, since);
            
            long queryTime = System.currentTimeMillis() - startTime;
            logger.info("Statistics query completed in {}ms - total: {}, severities: {}, services: {}", 
                       queryTime, totalCount, severityBreakdown.size(), serviceBreakdown.size());
            
            return response;
        } catch (Exception e) {
            long queryTime = System.currentTimeMillis() - startTime;
            logger.error("Statistics query failed after {}ms: {}", queryTime, e.getMessage());
            throw new RuntimeException("Failed to fetch statistics", e);
        }
    }
    
    private AnomalyResponse convertToResponse(Anomaly anomaly) {
        return new AnomalyResponse(
            anomaly.getId(),
            anomaly.getEventId(),
            anomaly.getServiceName(),
            anomaly.getSeverity().name(),
            anomaly.getReason(),
            anomaly.getRecommendedAction(),
            anomaly.getDetectedAt()
        );
    }
}
