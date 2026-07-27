package com.monitoring.security.repository;

import com.monitoring.security.entity.SecurityIncident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SecurityIncidentRepository extends JpaRepository<SecurityIncident, String> {
    
    List<SecurityIncident> findByUserIdOrderByCreatedAtDesc(String userId);
    
    List<SecurityIncident> findByIncidentTypeOrderByCreatedAtDesc(String incidentType);
    
    List<SecurityIncident> findBySeverityOrderByCreatedAtDesc(String severity);
    
    List<SecurityIncident> findByResolvedFalseOrderByCreatedAtDesc();
    
    List<SecurityIncident> findByCorrelatedAnomalyId(String anomalyId);
    
    List<SecurityIncident> findBySeverityAndResolvedOrderByCreatedAtDesc(String severity, Boolean resolved);

    @Query("SELECT i FROM SecurityIncident i WHERE " +
           "(:severity IS NULL OR i.severity = :severity) AND " +
           "(:resolved IS NULL OR i.resolved = :resolved)")
    Page<SecurityIncident> findByFilters(@Param("severity") String severity,
                                         @Param("resolved") Boolean resolved,
                                         Pageable pageable);
}
