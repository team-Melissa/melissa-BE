-- STAB-004 apply script
-- Target DB: MySQL 8.x
-- Purpose:
-- 1) add explicit image state column on diary
-- 2) backfill legacy rows
-- 3) enforce NOT NULL + DEFAULT + CHECK guard

-- =========================
-- A. Fail-fast assertion (only when column already exists)
-- =========================
DROP PROCEDURE IF EXISTS sp_stab004_assert;
DELIMITER //
CREATE PROCEDURE sp_stab004_assert()
BEGIN
    DECLARE v_has_col BIGINT DEFAULT 0;
    DECLARE v_invalid BIGINT DEFAULT 0;

    SELECT COUNT(*) INTO v_has_col
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'diary'
      AND column_name = 'image_status';

    IF v_has_col > 0 THEN
        SELECT COUNT(*) INTO v_invalid
        FROM diary
        WHERE image_status IS NOT NULL
          AND image_status NOT IN ('NONE', 'PENDING', 'READY', 'FAILED');

        IF v_invalid > 0 THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'STAB-004 blocked: diary.image_status contains invalid values';
        END IF;
    END IF;
END //
DELIMITER ;

CALL sp_stab004_assert();
DROP PROCEDURE IF EXISTS sp_stab004_assert;

-- =========================
-- B. Add column (idempotent)
-- =========================
SET @has_col := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'diary'
      AND column_name = 'image_status'
);

SET @sql := IF(
    @has_col = 0,
    'ALTER TABLE diary ADD COLUMN image_status VARCHAR(20) NULL AFTER image_url',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- =========================
-- C. Backfill legacy rows
-- Rule:
-- - image_url is present  -> READY
-- - image_url is missing  -> NONE
-- =========================
UPDATE diary
SET image_status = CASE
    WHEN image_url IS NULL OR TRIM(image_url) = '' THEN 'NONE'
    ELSE 'READY'
END
WHERE image_status IS NULL;

-- =========================
-- D. Enforce NOT NULL + DEFAULT
-- =========================
ALTER TABLE diary
    MODIFY COLUMN image_status VARCHAR(20) NOT NULL DEFAULT 'NONE';

-- =========================
-- E. Add CHECK guard (idempotent)
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
    @has_chk = 0,
    'ALTER TABLE diary ADD CONSTRAINT chk_diary_image_status CHECK (image_status IN (''NONE'', ''PENDING'', ''READY'', ''FAILED''))',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

