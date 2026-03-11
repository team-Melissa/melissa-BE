-- STAB-005 pre-check (run before apply script)
-- Target DB: MySQL 8.x
-- Purpose: inspect notification delivery-state migration readiness

-- 1) Baseline rows
SELECT COUNT(*) AS user_setting_total_count
FROM user_setting;

-- 2) Column existence
SELECT COUNT(*) AS has_last_attempt_at_column
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'user_setting'
  AND column_name = 'last_attempt_at';

SELECT COUNT(*) AS has_retry_count_column
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'user_setting'
  AND column_name = 'retry_count';

-- 3) If retry_count exists, inspect null/invalid distribution
SET @has_retry_count_col := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'user_setting'
      AND column_name = 'retry_count'
);

SET @sql := IF(
    @has_retry_count_col = 1,
    'SELECT COUNT(*) AS retry_count_null_count FROM user_setting WHERE retry_count IS NULL',
    'SELECT NULL AS retry_count_null_count'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
    @has_retry_count_col = 1,
    'SELECT COUNT(*) AS retry_count_negative_count FROM user_setting WHERE retry_count < 0',
    'SELECT NULL AS retry_count_negative_count'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
    @has_retry_count_col = 1,
    'SELECT retry_count, COUNT(*) AS cnt FROM user_setting GROUP BY retry_count ORDER BY retry_count LIMIT 20',
    'SELECT ''retry_count column not found'' AS note'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 4) Existing indexes
SELECT table_name, index_name, column_name, seq_in_index
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND table_name = 'user_setting'
ORDER BY index_name, seq_in_index;
