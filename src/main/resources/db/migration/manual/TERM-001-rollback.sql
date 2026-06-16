-- TERM-001 rollback script
-- Target DB: MySQL 8.x

SET @has_user_term_agreement := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'user_term_agreement'
);

SET @sql := IF(@has_user_term_agreement > 0, 'DROP TABLE user_term_agreement', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_term_version := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'term_version'
);

SET @sql := IF(@has_term_version > 0, 'DROP TABLE term_version', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_term := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'term'
);

SET @sql := IF(@has_term > 0, 'DROP TABLE term', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
