package com.monitoring.security.repository;

import com.monitoring.security.entity.SecurityAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SecurityAuditRepository extends JpaRepository<SecurityAudit, String> {
    
    List<SecurityAudit> findByUserIdOrderByTimestampDesc(String userId);
    
    List<SecurityAudit> findByActionOrderByTimestampDesc(String action);
    
    List<SecurityAudit> findByStatusOrderByTimestampDesc(String status);
    
    List<SecurityAudit> findByTimestampBetween(LocalDateTime start, LocalDateTime end);
}
