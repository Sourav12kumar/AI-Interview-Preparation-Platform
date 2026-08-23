CREATE TABLE audit_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    actor_user_id BIGINT NULL,
    action VARCHAR(80) NOT NULL,
    target_type VARCHAR(60) NOT NULL,
    target_id VARCHAR(190) NULL,
    outcome VARCHAR(20) NOT NULL,
    correlation_id VARCHAR(64) NULL,
    metadata JSON NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT chk_audit_outcome CHECK (outcome IN ('SUCCESS', 'FAILURE')),
    CONSTRAINT fk_audit_actor FOREIGN KEY (actor_user_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_audit_created (created_at),
    INDEX idx_audit_actor_created (actor_user_id, created_at),
    INDEX idx_audit_action_created (action, created_at)
) ENGINE=InnoDB;
