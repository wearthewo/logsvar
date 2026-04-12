package com.monitoring.backend.scheduled;

import com.monitoring.backend.repository.MonitoringEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class EventCleanupScheduler {
    
    private static final Logger logger = LoggerFactory.getLogger(EventCleanupScheduler.class);
    
    private final MonitoringEventRepository eventRepository;
    
    public EventCleanupScheduler(MonitoringEventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }
    
    @Scheduled(cron = "0 0 2 * * *")  // 2am daily
    public void purgeOldEvents() {
        logger.info("Starting cleanup of old monitoring events");
        
        try {
            Instant cutoffDate = Instant.now().minusSeconds(30 * 24 * 60 * 60); // 30 days ago
            
            long deletedCount = eventRepository.deleteByReceivedAtBefore(cutoffDate);
            
            logger.info("Cleanup completed: {} events older than {} deleted", 
                       deletedCount, cutoffDate);
            
        } catch (Exception e) {
            logger.error("Failed to cleanup old events: {}", e.getMessage(), e);
        }
    }
}
