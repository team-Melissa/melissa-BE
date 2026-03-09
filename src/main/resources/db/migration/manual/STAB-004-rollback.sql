-- STAB-004 rollback script
-- Target DB: MySQL 8.x
-- Note: rollback drops state guard and state column introduced by STAB-004.

-- =========================
-- A. Drop CHECK constraint (if present)
-- =========================
SET @has_chk := (
    SELECT COUNT(*)
    FROM information_schema.table_constraints
    WHERE table_schema = DATABASE()
      AND table_name = 'diary'
      AND constraint_name = 'chk_diary_image_status'
      AND constraint_type = 'CHECK'
);

SET @sql := IF(
    @has_chk > 0,
    'ALTER TABLE diary DROP CHECK chk_diary_image_status',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- =========================
-- B. Drop column (if present)
-- =========================
SET @has_col := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'diary'
      AND column_name = 'image_status'
);

SET @sql := IF(
    @has_col > 0,
    'ALTER TABLE diary DROP COLUMN image_status',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

