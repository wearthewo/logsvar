package com.monitoring.security.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaTopicsConfig {

    @Value("${app.kafka.topics.security:security.events}")
    public String securityTopic;

    @Value("${app.kafka.topics.audit:security.audit}")
    public String auditTopic;
}
