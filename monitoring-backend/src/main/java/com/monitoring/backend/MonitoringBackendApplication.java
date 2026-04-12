package com.monitoring.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MonitoringBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(MonitoringBackendApplication.class, args);
    }
}
