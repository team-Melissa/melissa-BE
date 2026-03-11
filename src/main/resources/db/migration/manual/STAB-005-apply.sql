-- STAB-005 apply script
-- Target DB: MySQL 8.x
-- Purpose:
-- 1) add notification delivery-state columns on user_setting
-- 2) backfill and enforce retry_count default/non-null
-- 3) add retry targeting index

-- =========================
-- A. Add last_attempt_at column (idempotent)
-- =========================
SET @has_last_attempt_col := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'user_setting'
      AND column_name = 'last_attempt_at'
);

SET @sql := IF(
    @has_last_attempt_col = 0,
    'ALTER TABLE user_setting ADD COLUMN last_attempt_at DATETIME NULL AFTER last_sent_date',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- =========================
-- B. Add retry_count column (idempotent)
-- =========================
SET @has_retry_count_col := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'user_setting'
      AND column_name = 'retry_count'
);

SET @sql := IF(
    @has_retry_count_col = 0,
    'ALTER TABLE user_setting ADD COLUMN retry_count INT NULL DEFAULT 0 AFTER last_attempt_at',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- =========================
-- C. Backfill + enforce retry_count
-- =========================
UPDATE user_setting
SET retry_count = 0
WHERE retry_count IS NULL OR retry_count < 0;

ALTER TABLE user_setting
    MODIFY COLUMN retry_count INT NOT NULL DEFAULT 0;

-- =========================
-- D. Add retry targeting index (idempotent)
-- =========================
SET @has_retry_idx := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'user_setting'
      AND index_name = 'idx_user_setting_retry_target'
);

SET @sql := IF(
    @has_retry_idx = 0,
    'CREATE INDEX idx_user_setting_retry_target ON user_setting (notification_enabled, last_sent_date, last_attempt_at, retry_count, notification_time, user_id)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
