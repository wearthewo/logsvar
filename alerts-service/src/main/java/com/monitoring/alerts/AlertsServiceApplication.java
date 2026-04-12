package com.monitoring.alerts;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class AlertsServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AlertsServiceApplication.class, args);
    }
}
