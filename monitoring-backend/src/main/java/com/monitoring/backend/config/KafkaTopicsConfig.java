package com.monitoring.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaTopicsConfig {

    @Value("${app.kafka.topics.events:monitoring.events}")
    public String eventsTopic;

    @Value("${app.kafka.topics.anomalies:monitoring.anomalies}")
    public String anomaliesTopic;

    @Value("${app.kafka.topics.alerts:monitoring.alerts}")
    public String alertsTopic;

    @Value("${app.kafka.topics.security:security.events}")
    public String securityTopic;
}
