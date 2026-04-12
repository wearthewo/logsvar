package com.monitoring.alerts.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaTopicsConfig {

    @Value("${app.kafka.topics.anomalies:monitoring.anomalies}")
    public String anomaliesTopic;

    @Value("${app.kafka.topics.alerts:monitoring.alerts}")
    public String alertsTopic;

    @Value("${app.kafka.topics.dlq:alerts.dlq}")
    public String dlqTopic;
}
