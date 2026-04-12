-- Deployment Audit Table
CREATE TABLE deployment_audit (
    id VARCHAR(36) PRIMARY KEY DEFAULT (UUID()),
    service VARCHAR(100) NOT NULL,
    version VARCHAR(50) NOT NULL,
    environment VARCHAR(20) NOT NULL,
    triggered_by VARCHAR(255) NOT NULL,
    commit_sha VARCHAR(40),
    status VARCHAR(20) NOT NULL,
    reason TEXT,
    deploy_duration_ms BIGINT,
    rollout_status VARCHAR(20),
    pod_readiness_time_ms BIGINT,
    health_check_status VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP NULL,
    metadata JSON,
    
    -- Indexes for querying
    INDEX idx_service_env (service, environment),
    INDEX idx_environment_status (environment, status),
    INDEX idx_created_at (created_at),
    INDEX idx_triggered_by (triggered_by),
    INDEX idx_version (version),
    INDEX idx_commit_sha (commit_sha)
);

-- Deployment Events Table (for detailed event tracking)
CREATE TABLE deployment_events (
    id VARCHAR(36) PRIMARY KEY DEFAULT (UUID()),
    deployment_id VARCHAR(36) NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    event_status VARCHAR(20) NOT NULL,
    message TEXT,
    details JSON,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign key to deployment_audit
    FOREIGN KEY (deployment_id) REFERENCES deployment_audit(id) ON DELETE CASCADE,
    
    -- Indexes
    INDEX idx_deployment_id (deployment_id),
    INDEX idx_event_type (event_type),
    INDEX idx_timestamp (timestamp)
);

-- Deployment Health Checks Table
CREATE TABLE deployment_health_checks (
    id VARCHAR(36) PRIMARY KEY DEFAULT (UUID()),
    deployment_id VARCHAR(36) NOT NULL,
    service_name VARCHAR(100) NOT NULL,
    check_type VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    response_time_ms BIGINT,
    error_message TEXT,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign key to deployment_audit
    FOREIGN KEY (deployment_id) REFERENCES deployment_audit(id) ON DELETE CASCADE,
    
    -- Indexes
    INDEX idx_deployment_id (deployment_id),
    INDEX idx_service_name (service_name),
    INDEX idx_check_type (check_type),
    INDEX idx_status (status),
    INDEX idx_timestamp (timestamp)
);

-- Views for common queries
CREATE VIEW deployment_summary AS
SELECT 
    da.id,
    da.service,
    da.version,
    da.environment,
    da.triggered_by,
    da.status,
    da.deploy_duration_ms,
    da.created_at,
    da.completed_at,
    CASE 
        WHEN da.status = 'success' THEN 'Success'
        WHEN da.status = 'failure' THEN 'Failed'
        WHEN da.status = 'rollback' THEN 'Rolled Back'
        ELSE 'In Progress'
    END as status_label,
    COUNT(de.id) as event_count
FROM deployment_audit da
LEFT JOIN deployment_events de ON da.id = de.deployment_id
GROUP BY da.id, da.service, da.version, da.environment, da.triggered_by, da.status, da.deploy_duration_ms, da.created_at, da.completed_at;

CREATE VIEW recent_deployments AS
SELECT 
    service,
    environment,
    version,
    status,
    triggered_by,
    created_at,
    deploy_duration_ms,
    RANK() OVER (PARTITION BY service, environment ORDER BY created_at DESC) as deployment_rank
FROM deployment_audit
WHERE created_at > DATE_SUB(NOW(), INTERVAL 30 DAY);

-- Stored procedure for recording deployment
DELIMITER //
CREATE PROCEDURE RecordDeployment(
    IN p_service VARCHAR(100),
    IN p_version VARCHAR(50),
    IN p_environment VARCHAR(20),
    IN p_triggered_by VARCHAR(255),
    IN p_commit_sha VARCHAR(40),
    IN p_status VARCHAR(20),
    IN p_reason TEXT,
    IN p_deploy_duration_ms BIGINT,
    IN p_rollout_status VARCHAR(20),
    IN p_pod_readiness_time_ms BIGINT,
    IN p_health_check_status VARCHAR(20),
    IN p_metadata JSON
)
BEGIN
    DECLARE deployment_id VARCHAR(36);
    
    -- Set deployment ID
    SET deployment_id = UUID();
    
    -- Insert deployment record
    INSERT INTO deployment_audit (
        id, service, version, environment, triggered_by, commit_sha, status,
        reason, deploy_duration_ms, rollout_status, pod_readiness_time_ms,
        health_check_status, metadata, completed_at
    ) VALUES (
        deployment_id, p_service, p_version, p_environment, p_triggered_by,
        p_commit_sha, p_status, p_reason, p_deploy_duration_ms,
        p_rollout_status, p_pod_readiness_time_ms, p_health_check_status,
        p_metadata, 
        CASE WHEN p_status IN ('success', 'failure', 'rollback') THEN NOW() ELSE NULL END
    );
    
    -- Record initial event
    INSERT INTO deployment_events (deployment_id, event_type, event_status, message)
    VALUES (deployment_id, 'deployment_started', p_status, 
            CONCAT('Deployment of ', p_service, ' version ', p_version, ' started'));
    
    -- Return deployment ID
    SELECT deployment_id as deployment_id;
END //
DELIMITER ;

-- Stored procedure for updating deployment status
DELIMITER //
CREATE PROCEDURE UpdateDeploymentStatus(
    IN p_deployment_id VARCHAR(36),
    IN p_status VARCHAR(20),
    IN p_rollout_status VARCHAR(20),
    IN p_deploy_duration_ms BIGINT,
    IN p_message TEXT
)
BEGIN
    -- Update deployment record
    UPDATE deployment_audit 
    SET status = p_status,
        rollout_status = p_rollout_status,
        deploy_duration_ms = p_deploy_duration_ms,
        completed_at = CASE WHEN p_status IN ('success', 'failure', 'rollback') THEN NOW() ELSE completed_at END
    WHERE id = p_deployment_id;
    
    -- Record event
    INSERT INTO deployment_events (deployment_id, event_type, event_status, message)
    VALUES (p_deployment_id, 'deployment_updated', p_status, p_message);
END //
DELIMITER ;

-- Function to get latest deployment version
DELIMITER //
CREATE FUNCTION GetLatestDeploymentVersion(
    p_service VARCHAR(100),
    p_environment VARCHAR(20)
) RETURNS VARCHAR(50)
READS SQL DATA
DETERMINISTIC
BEGIN
    DECLARE latest_version VARCHAR(50);
    
    SELECT version INTO latest_version
    FROM deployment_audit
    WHERE service = p_service AND environment = p_environment AND status = 'success'
    ORDER BY created_at DESC
    LIMIT 1;
    
    RETURN COALESCE(latest_version, '');
END //
DELIMITER ;
