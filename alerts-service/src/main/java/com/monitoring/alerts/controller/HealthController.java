package com.monitoring.alerts.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
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
        Map<String, Object> health = Map.of(
            "status", "UP",
            "components", Map.of(
                "db", checkDatabase(),
                "redis", checkRedis(),
                "kafka", checkKafka()
            )
        );
        return ResponseEntity.ok(health);
    }
    
    private Map<String, String> checkDatabase() {
        try (Connection connection = dataSource.getConnection()) {
            return Map.of("status", "UP");
        } catch (Exception e) {
            return Map.of("status", "DOWN", "details", e.getMessage());
        }
    }
    
    private Map<String, String> checkRedis() {
        try {
            redisTemplate.opsForValue().get("health-check");
            return Map.of("status", "UP");
        } catch (Exception e) {
            return Map.of("status", "DOWN", "details", e.getMessage());
        }
    }
    
    private Map<String, String> checkKafka() {
        try {
            kafkaTemplate.getProducerFactory().createProducer();
            return Map.of("status", "UP");
        } catch (Exception e) {
            return Map.of("status", "DOWN", "details", e.getMessage());
        }
    }
}
