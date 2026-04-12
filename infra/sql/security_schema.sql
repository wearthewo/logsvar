-- Security Audit Table
CREATE TABLE security_audit (
    id VARCHAR(36) PRIMARY KEY DEFAULT (UUID()),
    user_id VARCHAR(255) NOT NULL,
    action VARCHAR(100) NOT NULL,
    resource VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL,
    ip_address VARCHAR(45) NOT NULL,
    route VARCHAR(255),
    method VARCHAR(10),
    status_code INT,
    latency_ms BIGINT,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    user_agent TEXT,
    INDEX idx_user_timestamp (user_id, timestamp),
    INDEX idx_action_timestamp (action, timestamp),
    INDEX idx_status_timestamp (status, timestamp),
    INDEX idx_timestamp (timestamp)
);

-- Security Incidents Table
CREATE TABLE security_incidents (
    id VARCHAR(36) PRIMARY KEY DEFAULT (UUID()),
    user_id VARCHAR(255),
    incident_type VARCHAR(50) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    route VARCHAR(255),
    ip_address VARCHAR(45) NOT NULL,
    description TEXT,
    metadata JSON,
    correlated_anomaly_id VARCHAR(36),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    resolved BOOLEAN DEFAULT FALSE,
    INDEX idx_user_timestamp (user_id, created_at),
    INDEX idx_type_timestamp (incident_type, created_at),
    INDEX idx_severity_timestamp (severity, created_at),
    INDEX idx_correlated_anomaly (correlated_anomaly_id),
    INDEX idx_resolved (resolved),
    INDEX idx_created_at (created_at)
);

-- Security Events Kafka Topic (for reference)
-- Topic: security.events
-- Schema: JSON with userId, type, route, ip, timestamp, metadata
