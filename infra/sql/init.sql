-- Core Database Schema for Monitoring Application
-- This file creates the main tables for the monitoring system

-- Monitoring Events Table
CREATE TABLE IF NOT EXISTS monitoring_events (
    id VARCHAR(36) PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL,
    service_name VARCHAR(100) NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    payload JSON NOT NULL,
    host VARCHAR(255),
    severity VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_service_timestamp (service_name, timestamp),
    INDEX idx_event_type (event_type),
    INDEX idx_timestamp (timestamp),
    INDEX idx_severity (severity)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Anomalies Table
CREATE TABLE IF NOT EXISTS anomalies (
    id VARCHAR(36) PRIMARY KEY,
    event_id VARCHAR(36) NOT NULL,
    service_name VARCHAR(100) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    reason TEXT NOT NULL,
    recommended_action TEXT,
    detected_at TIMESTAMP NOT NULL,
    resolved BOOLEAN DEFAULT FALSE,
    resolved_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (event_id) REFERENCES monitoring_events(id) ON DELETE CASCADE,
    INDEX idx_service_severity (service_name, severity),
    INDEX idx_detected_at (detected_at),
    INDEX idx_resolved (resolved),
    INDEX idx_event_id (event_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Alert Rules Table
CREATE TABLE IF NOT EXISTS alert_rules (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    service_name VARCHAR(100),
    event_type VARCHAR(50),
    condition_expression TEXT NOT NULL,
    severity_threshold VARCHAR(20) NOT NULL,
    enabled BOOLEAN DEFAULT TRUE,
    notification_channels JSON,
    created_by VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_service_enabled (service_name, enabled),
    INDEX idx_event_type (event_type),
    INDEX idx_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Alert History Table
CREATE TABLE IF NOT EXISTS alert_history (
    id VARCHAR(36) PRIMARY KEY,
    rule_id VARCHAR(36) NOT NULL,
    anomaly_id VARCHAR(36),
    service_name VARCHAR(100) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    message TEXT,
    notification_sent BOOLEAN DEFAULT FALSE,
    notification_channels JSON,
    triggered_at TIMESTAMP NOT NULL,
    resolved_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (rule_id) REFERENCES alert_rules(id) ON DELETE CASCADE,
    FOREIGN KEY (anomaly_id) REFERENCES anomalies(id) ON DELETE SET NULL,
    INDEX idx_rule_id (rule_id),
    INDEX idx_service_status (service_name, status),
    INDEX idx_triggered_at (triggered_at),
    INDEX idx_severity (severity)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
