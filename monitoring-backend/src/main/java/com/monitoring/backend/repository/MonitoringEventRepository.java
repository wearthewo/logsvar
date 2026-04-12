package com.monitoring.backend.repository;

import com.monitoring.backend.entity.MonitoringEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface MonitoringEventRepository extends JpaRepository<MonitoringEvent, String> {
    
    List<MonitoringEvent> findByServiceNameOrderByReceivedAtDesc(String serviceName);
    
    List<MonitoringEvent> findByEventTypeOrderByReceivedAtDesc(MonitoringEvent.EventType eventType);
    
    @Query("SELECT e FROM MonitoringEvent e WHERE e.receivedAt < :before")
    @Modifying
    int deleteByReceivedAtBefore(@Param("before") Instant before);
}
