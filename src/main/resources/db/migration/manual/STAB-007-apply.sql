-- STAB-007 apply script
-- Target DB: MySQL 8.x
-- Purpose: add async job and outbox storage for SQS-backed diary image generation

SET @has_async_job := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'async_job'
);

SET @sql := IF(
    @has_async_job = 0,
    'CREATE TABLE async_job (
        id BIGINT NOT NULL AUTO_INCREMENT,
        job_type VARCHAR(64) NOT NULL,
        target_type VARCHAR(64) NOT NULL,
        target_id BIGINT NOT NULL,
        target_version INT NULL,
        dedupe_key VARCHAR(180) NOT NULL,
        status VARCHAR(32) NOT NULL,
        attempt_count INT NOT NULL DEFAULT 0,
        last_error VARCHAR(500) NULL,
        started_at DATETIME NULL,
        completed_at DATETIME NULL,
        created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        version BIGINT NOT NULL DEFAULT 0,
        PRIMARY KEY (id),
        CONSTRAINT uk_async_job_dedupe_key UNIQUE (dedupe_key),
        INDEX idx_async_job_status (status),
        INDEX idx_async_job_target (target_type, target_id)
    )',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_outbox_event := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'outbox_event'
);

SET @sql := IF(
    @has_outbox_event = 0,
    'CREATE TABLE outbox_event (
        id BIGINT NOT NULL AUTO_INCREMENT,
        event_type VARCHAR(64) NOT NULL,
        job_id BIGINT NOT NULL,
        target_id BIGINT NOT NULL,
        payload_json TEXT NOT NULL,
        status VARCHAR(32) NOT NULL,
        publish_attempt_count INT NOT NULL DEFAULT 0,
        next_publish_at DATETIME NULL,
        published_at DATETIME NULL,
        sqs_message_id VARCHAR(120) NULL,
        last_error VARCHAR(500) NULL,
        created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        version BIGINT NOT NULL DEFAULT 0,
        PRIMARY KEY (id),
        INDEX idx_outbox_status_next_publish_at (status, next_publish_at),
        INDEX idx_outbox_job_id (job_id),
        CONSTRAINT fk_outbox_event_async_job
            FOREIGN KEY (job_id) REFERENCES async_job(id)
            ON DELETE CASCADE
    )',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
