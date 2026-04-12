package com.monitoring.security.repository;

import com.monitoring.security.entity.SecurityIncident;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
