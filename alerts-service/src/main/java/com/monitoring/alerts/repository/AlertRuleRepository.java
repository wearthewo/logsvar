package com.monitoring.alerts.repository;

import com.monitoring.alerts.model.AlertRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AlertRuleRepository extends JpaRepository<AlertRule, String> {
    
    List<AlertRule> findByEnabledTrue();
    
    List<AlertRule> findByChannel(AlertRule.Channel channel);
    
    @Query("SELECT r FROM AlertRule r WHERE r.enabled = true AND " +
           "(r.serviceFilter IS NULL OR r.serviceFilter = :serviceName)")
    List<AlertRule> findEnabledRulesForService(@Param("serviceName") String serviceName);
    
    Optional<AlertRule> findByIdAndEnabledTrue(String id);
}
