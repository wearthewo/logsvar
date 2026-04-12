package com.monitoring.alerts;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration",
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
    "spring.autoconfigure.exclude=org.springframework.kafka.autoconfigure.KafkaAutoConfiguration"
})
class AlertsServiceApplicationTests {
    @Test
    void contextLoads() {
    }
}
