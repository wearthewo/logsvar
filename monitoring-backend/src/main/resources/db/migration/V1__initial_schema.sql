CREATE TABLE IF NOT EXISTS monitoring_events (
  id           CHAR(36)                                    NOT NULL,
  service_name VARCHAR(128)                                NOT NULL,
  event_type   ENUM('HTTP_REQUEST','EXCEPTION',
                    'SYSTEM_METRIC','DB_QUERY')            NOT NULL,
  payload      JSON                                        NOT NULL,
  received_at  DATETIME(3)                                 NOT NULL
               DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  INDEX idx_service_time (service_name, received_at),
  INDEX idx_event_type   (event_type),
  INDEX idx_received_at  (received_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS anomalies (
  id                 CHAR(36)                                        NOT NULL,
  event_id           CHAR(36)                                        NOT NULL,
  service_name       VARCHAR(128)                                    NOT NULL,
  severity           ENUM('LOW','MEDIUM','HIGH','CRITICAL')          NOT NULL,
  reason             TEXT                                            NOT NULL,
  recommended_action TEXT,
  detected_at        DATETIME(3)                                     NOT NULL
                     DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  INDEX idx_service_severity (service_name, severity),
  INDEX idx_detected_at      (detected_at),
  INDEX idx_event_id         (event_id),
  INDEX idx_service_detected (service_name, detected_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
