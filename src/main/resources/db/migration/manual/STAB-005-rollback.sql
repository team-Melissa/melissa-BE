-- STAB-005 rollback script
-- Target DB: MySQL 8.x
-- Note: rollback drops delivery-state index and columns introduced by STAB-005.

-- =========================
-- A. Drop retry index (if present)
-- =========================
SET @has_retry_idx := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'user_setting'
      AND index_name = 'idx_user_setting_retry_target'
);

SET @sql := IF(
    @has_retry_idx > 0,
    'DROP INDEX idx_user_setting_retry_target ON user_setting',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- =========================
-- B. Drop retry_count column (if present)
-- =========================
SET @has_retry_count_col := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'user_setting'
      AND column_name = 'retry_count'
);

SET @sql := IF(
    @has_retry_count_col > 0,
    'ALTER TABLE user_setting DROP COLUMN retry_count',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- =========================
-- C. Drop last_attempt_at column (if present)
-- =========================
SET @has_last_attempt_col := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'user_setting'
      AND column_name = 'last_attempt_at'
);

SET @sql := IF(
    @has_last_attempt_col > 0,
    'ALTER TABLE user_setting DROP COLUMN last_attempt_at',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
