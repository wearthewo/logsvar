package com.monitoring.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
public class HealthController {
    
    @Autowired
    private DataSource dataSource;
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    
    @GetMapping("/actuator/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> dbHealth = checkDatabase();
        Map<String, Object> redisHealth = checkRedis();
        Map<String, Object> kafkaHealth = checkKafka();
        
        // Determine overall status
        boolean allUp = "UP".equals(dbHealth.get("status")) && 
                       "UP".equals(redisHealth.get("status")) && 
                       "UP".equals(kafkaHealth.get("status"));
        
        Map<String, Object> health = new HashMap<>();
        health.put("status", allUp ? "UP" : "DOWN");
        health.put("timestamp", LocalDateTime.now().toString());
        health.put("components", Map.of(
            "db", dbHealth,
            "redis", redisHealth,
            "kafka", kafkaHealth
        ));
        
        return ResponseEntity.ok(health);
    }
    
    private Map<String, Object> checkDatabase() {
        Map<String, Object> dbHealth = new HashMap<>();
        try (Connection connection = dataSource.getConnection()) {
            dbHealth.put("status", "UP");
        } catch (Exception e) {
            dbHealth.put("status", "DOWN");
            dbHealth.put("details", e.getMessage());
        }
        return dbHealth;
    }
    
    private Map<String, Object> checkRedis() {
        Map<String, Object> redisHealth = new HashMap<>();
        try {
            redisTemplate.opsForValue().get("health-check");
            redisHealth.put("status", "UP");
        } catch (Exception e) {
            redisHealth.put("status", "DOWN");
            redisHealth.put("details", e.getMessage());
        }
        return redisHealth;
    }
    
    private Map<String, Object> checkKafka() {
        Map<String, Object> kafkaHealth = new HashMap<>();
        try {
            kafkaTemplate.getProducerFactory().createProducer();
            kafkaHealth.put("status", "UP");
        } catch (Exception e) {
            kafkaHealth.put("status", "DOWN");
            kafkaHealth.put("details", e.getMessage());
        }
        return kafkaHealth;
    }
}
