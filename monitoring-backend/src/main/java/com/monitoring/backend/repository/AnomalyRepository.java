package com.monitoring.backend.repository;

import com.monitoring.backend.entity.Anomaly;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface AnomalyRepository extends JpaRepository<Anomaly, String> {
    
    // Primary query using idx_detected_at index for newest first ordering
    @Query("SELECT a FROM Anomaly a ORDER BY a.detectedAt DESC")
    Page<Anomaly> findAllNewestFirst(Pageable pageable);
    
    // Query by service name using idx_service_detected index
    @Query("SELECT a FROM Anomaly a WHERE a.serviceName = :serviceName ORDER BY a.detectedAt DESC")
    Page<Anomaly> findByServiceNameNewestFirst(@Param("serviceName") String serviceName, Pageable pageable);
    
    // Query by severity using idx_service_severity index
    @Query("SELECT a FROM Anomaly a WHERE a.severity = :severity ORDER BY a.detectedAt DESC")
    Page<Anomaly> findBySeverityNewestFirst(@Param("severity") Anomaly.Severity severity, Pageable pageable);
    
    // Time range query using idx_detected_at index
    @Query("SELECT a FROM Anomaly a WHERE a.detectedAt >= :from AND a.detectedAt <= :to ORDER BY a.detectedAt DESC")
    Page<Anomaly> findByTimeRangeNewestFirst(@Param("from") Instant from, @Param("to") Instant to, Pageable pageable);
    
    // Combined service + time range using idx_service_detected index
    @Query("SELECT a FROM Anomaly a WHERE a.serviceName = :serviceName AND a.detectedAt >= :from AND a.detectedAt <= :to ORDER BY a.detectedAt DESC")
    Page<Anomaly> findByServiceAndTimeRangeNewestFirst(@Param("serviceName") String serviceName, 
                                                     @Param("from") Instant from, 
                                                     @Param("to") Instant to, 
                                                     Pageable pageable);
    
    // Combined severity + time range using idx_service_severity and idx_detected_at
    @Query("SELECT a FROM Anomaly a WHERE a.severity = :severity AND a.detectedAt >= :from AND a.detectedAt <= :to ORDER BY a.detectedAt DESC")
    Page<Anomaly> findBySeverityAndTimeRangeNewestFirst(@Param("severity") Anomaly.Severity severity, 
                                                       @Param("from") Instant from, 
                                                       @Param("to") Instant to, 
                                                       Pageable pageable);
    
    // Complex filter query - optimized to use indexes efficiently
    @Query("SELECT a FROM Anomaly a WHERE " +
           "(:serviceName IS NULL OR a.serviceName = :serviceName) AND " +
           "(:severity IS NULL OR a.severity = :severity) AND " +
           "(:from IS NULL OR a.detectedAt >= :from) AND " +
           "(:to IS NULL OR a.detectedAt <= :to) " +
           "ORDER BY a.detectedAt DESC")
    Page<Anomaly> findByFiltersNewestFirst(@Param("serviceName") String serviceName,
                                         @Param("severity") Anomaly.Severity severity,
                                         @Param("from") Instant from,
                                         @Param("to") Instant to,
                                         Pageable pageable);
    
    // Single anomaly lookup using primary key
    @Query("SELECT a FROM Anomaly a WHERE a.id = :id")
    Anomaly findByIdAnomaly(@Param("id") String id);
    
    // Aggregation queries for statistics - using idx_detected_at index
    @Query("SELECT COUNT(a) FROM Anomaly a WHERE a.detectedAt >= :since")
    long countSince(@Param("since") Instant since);
    
    @Query("SELECT a.severity, COUNT(a) FROM Anomaly a WHERE a.detectedAt >= :since GROUP BY a.severity")
    List<Object[]> countBySeveritySince(@Param("since") Instant since);
    
    @Query("SELECT a.serviceName, COUNT(a) FROM Anomaly a WHERE a.detectedAt >= :since GROUP BY a.serviceName")
    List<Object[]> countByServiceSince(@Param("since") Instant since);
    
    // Service health queries - using idx_service_detected index
    @Query("SELECT a FROM Anomaly a WHERE a.serviceName = :serviceName AND a.detectedAt >= :since ORDER BY a.detectedAt DESC")
    List<Anomaly> findRecentByService(@Param("serviceName") String serviceName, @Param("since") Instant since);
    
    @Query("SELECT a FROM Anomaly a WHERE a.detectedAt >= :since ORDER BY a.detectedAt DESC")
    List<Anomaly> findRecentAll(@Param("since") Instant since);
    
    // Derived query method for finding anomalies by service name ordered by detection time
    List<Anomaly> findByServiceNameOrderByDetectedAtDesc(String serviceName);
}
