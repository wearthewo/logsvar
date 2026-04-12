-- Security Service Initial Schema
-- Create tables for security incidents and audit logs

CREATE TABLE IF NOT EXISTS security_incidents (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(255),
    incident_type VARCHAR(100) NOT NULL,
    severity VARCHAR(50),
    description TEXT,
    status VARCHAR(50),
    ip_address VARCHAR(45),
    user_agent TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP NULL,
    INDEX idx_user_id (user_id),
    INDEX idx_incident_type (incident_type),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS security_audit (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(255),
    action VARCHAR(100) NOT NULL,
    resource VARCHAR(255),
    status VARCHAR(50),
    details TEXT,
    ip_address VARCHAR(45),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_action (action),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
