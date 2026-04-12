package com.monitoring.alerts.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class DeduplicationService {
    
    private static final Logger logger = LoggerFactory.getLogger(DeduplicationService.class);
    private static final String DEDUP_KEY_PREFIX = "alert:dedup:";
    private static final Duration TTL = Duration.ofSeconds(300); // 5 minutes
    
    private final StringRedisTemplate redisTemplate;
    
    public DeduplicationService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    
    public boolean isDuplicated(String serviceName, String severity) {
        try {
            String key = DEDUP_KEY_PREFIX + serviceName + ":" + severity;
            Boolean exists = redisTemplate.hasKey(key);
            
            if (Boolean.TRUE.equals(exists)) {
                logger.debug("Alert deduplication key exists for {}:{}, skipping", serviceName, severity);
                return true;
            }
            
            return false;
            
        } catch (Exception e) {
            logger.error("Error checking deduplication for {}:{}: {}", serviceName, severity, e.getMessage());
            return false; // Assume not duplicated on error
        }
    }
    
    public void setDeduplicationKey(String serviceName, String severity) {
        try {
            String key = DEDUP_KEY_PREFIX + serviceName + ":" + severity;
            redisTemplate.opsForValue().set(key, "1", TTL);
            logger.debug("Set deduplication key for {}:{}", serviceName, severity);
            
        } catch (Exception e) {
            logger.error("Error setting deduplication key for {}:{}: {}", serviceName, severity, e.getMessage());
        }
    }
}
