-- STAB-004 pre-check (run before apply script)
-- Target DB: MySQL 8.x
-- Purpose: inspect diary image status migration readiness

-- 1) Baseline counts
SELECT COUNT(*) AS diary_total_count
FROM diary;

SELECT COUNT(*) AS diary_image_url_null_count
FROM diary
WHERE image_url IS NULL;

SELECT COUNT(*) AS diary_image_url_not_null_count
FROM diary
WHERE image_url IS NOT NULL;

-- 2) image_status column existence
SELECT COUNT(*) AS has_diary_image_status_column
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'diary'
  AND column_name = 'image_status';

-- 3) If column exists, inspect null/invalid/distribution
SET @has_col := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'diary'
      AND column_name = 'image_status'
);

SET @sql := IF(
    @has_col = 1,
    'SELECT COUNT(*) AS diary_image_status_null_count FROM diary WHERE image_status IS NULL',
    'SELECT NULL AS diary_image_status_null_count'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
    @has_col = 1,
    'SELECT COUNT(*) AS diary_image_status_invalid_count FROM diary WHERE image_status IS NOT NULL AND image_status NOT IN (''NONE'', ''PENDING'', ''READY'', ''FAILED'')',
    'SELECT NULL AS diary_image_status_invalid_count'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
    @has_col = 1,
    'SELECT image_status, COUNT(*) AS cnt FROM diary GROUP BY image_status ORDER BY image_status',
    'SELECT ''image_status column not found'' AS note'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 4) Existing constraints visibility
SELECT tc.table_name, tc.constraint_name, tc.constraint_type
FROM information_schema.table_constraints tc
WHERE tc.table_schema = DATABASE()
  AND tc.table_name = 'diary'
ORDER BY tc.constraint_type, tc.constraint_name;

