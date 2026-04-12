CREATE TABLE IF NOT EXISTS alert_rules (
  id                 CHAR(36)                                          NOT NULL,
  name               VARCHAR(128)                                      NOT NULL,
  enabled            BOOLEAN                                           NOT NULL DEFAULT TRUE,
  severity_threshold ENUM('LOW','MEDIUM','HIGH','CRITICAL')            NOT NULL,
  service_filter     VARCHAR(128)                                               DEFAULT NULL,
  channel            ENUM('EMAIL','SLACK','WEBHOOK','IN_APP')          NOT NULL,
  destination        TEXT                                                       DEFAULT NULL,
  created_at         DATETIME(3)                                       NOT NULL
                     DEFAULT CURRENT_TIMESTAMP(3),
  updated_at         DATETIME(3)                                       NOT NULL
                     DEFAULT CURRENT_TIMESTAMP(3)
                     ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  INDEX idx_enabled_channel (enabled, channel)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS alert_history (
  id            CHAR(36)                                          NOT NULL,
  anomaly_id    CHAR(36)                                          NOT NULL,
  rule_id       CHAR(36)                                          NOT NULL,
  channel       ENUM('EMAIL','SLACK','WEBHOOK','IN_APP')          NOT NULL,
  destination   TEXT                                                       DEFAULT NULL,
  status        ENUM('SENT','FAILED','DEDUPLICATED')              NOT NULL,
  error_message TEXT                                                       DEFAULT NULL,
  sent_at       DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  INDEX idx_anomaly_id (anomaly_id),
  INDEX idx_rule_id    (rule_id),
  INDEX idx_sent_at    (sent_at),
  INDEX idx_status     (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
