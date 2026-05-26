-- STAB-007 rollback script
-- Target DB: MySQL 8.x

SET @has_outbox_event := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'outbox_event'
);

SET @sql := IF(
    @has_outbox_event > 0,
    'DROP TABLE outbox_event',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_async_job := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'async_job'
);

SET @sql := IF(
    @has_async_job > 0,
    'DROP TABLE async_job',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
