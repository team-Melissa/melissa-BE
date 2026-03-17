-- STAB-006 rollback script
-- Target DB: MySQL 8.x

SET @has_table := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'idempotency_record'
);

SET @sql := IF(
    @has_table > 0,
    'DROP TABLE idempotency_record',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
