-- STAB-002 rollback script
-- Target DB: MySQL 8.x
-- Note: This reverts named constraints/indexes introduced by STAB-002 apply script.

-- =========================
-- A. Drop added indexes (if present)
-- =========================
SET @has_idx := (SELECT COUNT(*) FROM information_schema.statistics
                 WHERE table_schema = DATABASE()
                   AND table_name = 'daily_chat_log'
                   AND index_name = 'idx_daily_chat_log_thread_created');
SET @sql := IF(@has_idx > 0,
               'DROP INDEX idx_daily_chat_log_thread_created ON daily_chat_log',
               'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_idx := (SELECT COUNT(*) FROM information_schema.statistics
                 WHERE table_schema = DATABASE()
                   AND table_name = 'diary'
                   AND index_name = 'idx_diary_thread_active_created');
SET @sql := IF(@has_idx > 0,
               'DROP INDEX idx_diary_thread_active_created ON diary',
               'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_idx := (SELECT COUNT(*) FROM information_schema.statistics
                 WHERE table_schema = DATABASE()
                   AND table_name = 'diary'
                   AND index_name = 'idx_diary_feed_user_active_id');
SET @sql := IF(@has_idx > 0,
               'DROP INDEX idx_diary_feed_user_active_id ON diary',
               'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_idx := (SELECT COUNT(*) FROM information_schema.statistics
                 WHERE table_schema = DATABASE()
                   AND table_name = 'diary'
                   AND index_name = 'idx_diary_user_month_active_day_created');
SET @sql := IF(@has_idx > 0,
               'DROP INDEX idx_diary_user_month_active_day_created ON diary',
               'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_idx := (SELECT COUNT(*) FROM information_schema.statistics
                 WHERE table_schema = DATABASE()
                   AND table_name = 'diary'
                   AND index_name = 'idx_diary_user_day_active_created');
SET @sql := IF(@has_idx > 0,
               'DROP INDEX idx_diary_user_day_active_created ON diary',
               'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_idx := (SELECT COUNT(*) FROM information_schema.statistics
                 WHERE table_schema = DATABASE()
                   AND table_name = 'user_setting'
                   AND index_name = 'idx_user_setting_notification_target');
SET @sql := IF(@has_idx > 0,
               'DROP INDEX idx_user_setting_notification_target ON user_setting',
               'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_idx := (SELECT COUNT(*) FROM information_schema.statistics
                 WHERE table_schema = DATABASE()
                   AND table_name = 'user_setting'
                   AND index_name = 'ux_user_setting_user_id');
SET @sql := IF(@has_idx > 0,
               'DROP INDEX ux_user_setting_user_id ON user_setting',
               'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_idx := (SELECT COUNT(*) FROM information_schema.statistics
                 WHERE table_schema = DATABASE()
                   AND table_name = 'user_memory'
                   AND index_name = 'ux_user_memory_user_id');
SET @sql := IF(@has_idx > 0,
               'DROP INDEX ux_user_memory_user_id ON user_memory',
               'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- =========================
-- B. Drop added CHECK constraints (if present)
-- =========================
SET @has_chk := (SELECT COUNT(*) FROM information_schema.table_constraints
                 WHERE table_schema = DATABASE()
                   AND table_name = 'thread'
                   AND constraint_name = 'chk_thread_month'
                   AND constraint_type = 'CHECK');
SET @sql := IF(@has_chk > 0,
               'ALTER TABLE thread DROP CHECK chk_thread_month',
               'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_chk := (SELECT COUNT(*) FROM information_schema.table_constraints
                 WHERE table_schema = DATABASE()
                   AND table_name = 'thread'
                   AND constraint_name = 'chk_thread_day'
                   AND constraint_type = 'CHECK');
SET @sql := IF(@has_chk > 0,
               'ALTER TABLE thread DROP CHECK chk_thread_day',
               'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_chk := (SELECT COUNT(*) FROM information_schema.table_constraints
                 WHERE table_schema = DATABASE()
                   AND table_name = 'diary'
                   AND constraint_name = 'chk_diary_month'
                   AND constraint_type = 'CHECK');
SET @sql := IF(@has_chk > 0,
               'ALTER TABLE diary DROP CHECK chk_diary_month',
               'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_chk := (SELECT COUNT(*) FROM information_schema.table_constraints
                 WHERE table_schema = DATABASE()
                   AND table_name = 'diary'
                   AND constraint_name = 'chk_diary_day'
                   AND constraint_type = 'CHECK');
SET @sql := IF(@has_chk > 0,
               'ALTER TABLE diary DROP CHECK chk_diary_day',
               'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- =========================
-- C. Drop named FK constraints introduced by STAB-002 (if present)
-- =========================
SET @has_fk := (SELECT COUNT(*) FROM information_schema.table_constraints
                WHERE table_schema = DATABASE()
                  AND table_name = 'user_setting'
                  AND constraint_name = 'fk_user_setting_user'
                  AND constraint_type = 'FOREIGN KEY');
SET @sql := IF(@has_fk > 0,
               'ALTER TABLE user_setting DROP FOREIGN KEY fk_user_setting_user',
               'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_fk := (SELECT COUNT(*) FROM information_schema.table_constraints
                WHERE table_schema = DATABASE()
                  AND table_name = 'user_memory'
                  AND constraint_name = 'fk_user_memory_user'
                  AND constraint_type = 'FOREIGN KEY');
SET @sql := IF(@has_fk > 0,
               'ALTER TABLE user_memory DROP FOREIGN KEY fk_user_memory_user',
               'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- =========================
-- D. Optional nullable rollback (disabled by default)
-- =========================
-- Uncomment only when you intentionally want to relax constraints.
-- ALTER TABLE user_setting MODIFY COLUMN user_id BIGINT NULL;
-- ALTER TABLE user_memory MODIFY COLUMN user_id BIGINT NULL;

