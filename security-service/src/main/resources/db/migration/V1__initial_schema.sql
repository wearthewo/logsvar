CREATE TABLE security_incidents (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(255),
    incident_type VARCHAR(100) NOT NULL,
    severity VARCHAR(50) NOT NULL,
    route VARCHAR(500),
    ip_address VARCHAR(45) NOT NULL,
    description TEXT,
    metadata JSON,
    correlated_anomaly_id VARCHAR(36),
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    resolved BOOLEAN NOT NULL DEFAULT FALSE,
    INDEX idx_user_timestamp (user_id, created_at),
    INDEX idx_type_timestamp (incident_type, created_at),
    INDEX idx_severity_timestamp (severity, created_at),
    INDEX idx_correlated_anomaly (correlated_anomaly_id),
    INDEX idx_resolved (resolved),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE security_audit (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    action VARCHAR(100) NOT NULL,
    resource VARCHAR(500) NOT NULL,
    status VARCHAR(50) NOT NULL,
    ip_address VARCHAR(45) NOT NULL,
    route VARCHAR(500),
    method VARCHAR(16),
    status_code INT,
    latency_ms BIGINT,
    timestamp DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    user_agent TEXT,
    INDEX idx_user_timestamp (user_id, timestamp),
    INDEX idx_action_timestamp (action, timestamp),
    INDEX idx_status_timestamp (status, timestamp),
    INDEX idx_timestamp (timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
