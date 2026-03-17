-- STAB-006 apply script
-- Target DB: MySQL 8.x
-- Purpose: add idempotency storage for chat message APIs

SET @has_table := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'idempotency_record'
);

SET @sql := IF(
    @has_table = 0,
    'CREATE TABLE idempotency_record (
        id BIGINT NOT NULL AUTO_INCREMENT,
        user_id BIGINT NOT NULL,
        endpoint VARCHAR(120) NOT NULL,
        idempotency_key VARCHAR(128) NOT NULL,
        request_hash VARCHAR(64) NOT NULL,
        status VARCHAR(32) NOT NULL,
        response_body TEXT NULL,
        error_message VARCHAR(500) NULL,
        expires_at DATETIME NOT NULL,
        created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        version BIGINT NOT NULL DEFAULT 0,
        PRIMARY KEY (id),
        CONSTRAINT uk_idempotency_user_endpoint_key UNIQUE (user_id, endpoint, idempotency_key),
        INDEX idx_idempotency_expires_at (expires_at)
    )',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
