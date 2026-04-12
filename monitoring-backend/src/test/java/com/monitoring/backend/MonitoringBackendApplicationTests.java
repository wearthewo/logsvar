package com.monitoring.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration",
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
    "spring.autoconfigure.exclude=org.springframework.kafka.autoconfigure.KafkaAutoConfiguration"
})
class MonitoringBackendApplicationTests {
    @Test
    void contextLoads() {
    }
}
