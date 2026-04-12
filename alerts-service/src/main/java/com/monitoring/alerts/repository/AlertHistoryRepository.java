package com.monitoring.alerts.repository;

import com.monitoring.alerts.model.AlertHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface AlertHistoryRepository extends JpaRepository<AlertHistory, String> {
    
    Page<AlertHistory> findAll(Pageable pageable);
    
    List<AlertHistory> findByRuleId(String ruleId);
    
    List<AlertHistory> findByAnomalyId(String anomalyId);
    
    List<AlertHistory> findByStatus(AlertHistory.Status status);
    
    @Query("SELECT h FROM AlertHistory h WHERE h.sentAt >= :from AND h.sentAt <= :to")
    List<AlertHistory> findBySentAtBetween(@Param("from") Instant from, @Param("to") Instant to);
    
    @Query("SELECT h FROM AlertHistory h WHERE h.ruleId = :ruleId AND h.sentAt >= :from AND h.sentAt <= :to")
    List<AlertHistory> findByRuleIdAndSentAtBetween(@Param("ruleId") String ruleId, 
                                                 @Param("from") Instant from, @Param("to") Instant to);
}
