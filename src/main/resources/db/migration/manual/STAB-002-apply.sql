-- STAB-002 apply script
-- Purpose:
-- 1) strengthen NOT NULL / UNIQUE / FK constraints on core domain
-- 2) add CHECK constraints for month/day
-- 3) add supporting indexes for dominant query patterns
-- Target DB: MySQL 8.x

-- =========================
-- A. Fail-fast assertions
-- =========================
DROP PROCEDURE IF EXISTS sp_stab002_assert;
DELIMITER //
CREATE PROCEDURE sp_stab002_assert()
BEGIN
    DECLARE v_count BIGINT DEFAULT 0;

    SELECT COUNT(*) INTO v_count
    FROM user_setting
    WHERE user_id IS NULL;
    IF v_count > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'STAB-002 blocked: user_setting.user_id contains NULL';
    END IF;

    SELECT COUNT(*) INTO v_count
    FROM user_memory
    WHERE user_id IS NULL;
    IF v_count > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'STAB-002 blocked: user_memory.user_id contains NULL';
    END IF;

    SELECT COUNT(*) INTO v_count
    FROM (
        SELECT user_id
        FROM user_setting
        GROUP BY user_id
        HAVING COUNT(*) > 1
    ) d;
    IF v_count > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'STAB-002 blocked: duplicate user_setting.user_id found';
    END IF;

    SELECT COUNT(*) INTO v_count
    FROM (
        SELECT user_id
        FROM user_memory
        GROUP BY user_id
        HAVING COUNT(*) > 1
    ) d;
    IF v_count > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'STAB-002 blocked: duplicate user_memory.user_id found';
    END IF;

    SELECT COUNT(*) INTO v_count
    FROM user_setting s
    LEFT JOIN `user` u ON u.id = s.user_id
    WHERE u.id IS NULL;
    IF v_count > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'STAB-002 blocked: orphan rows found in user_setting.user_id';
    END IF;

    SELECT COUNT(*) INTO v_count
    FROM user_memory m
    LEFT JOIN `user` u ON u.id = m.user_id
    WHERE u.id IS NULL;
    IF v_count > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'STAB-002 blocked: orphan rows found in user_memory.user_id';
    END IF;

    SELECT COUNT(*) INTO v_count
    FROM thread
    WHERE month < 1 OR month > 12 OR day < 1 OR day > 31;
    IF v_count > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'STAB-002 blocked: invalid month/day rows found in thread';
    END IF;

    SELECT COUNT(*) INTO v_count
    FROM diary
    WHERE month < 1 OR month > 12 OR day < 1 OR day > 31;
    IF v_count > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'STAB-002 blocked: invalid month/day rows found in diary';
    END IF;
END //
DELIMITER ;

CALL sp_stab002_assert();
DROP PROCEDURE IF EXISTS sp_stab002_assert;

-- =========================
-- B. NOT NULL enforcement
-- =========================
ALTER TABLE user_setting
    MODIFY COLUMN user_id BIGINT NOT NULL;

ALTER TABLE user_memory
    MODIFY COLUMN user_id BIGINT NOT NULL;

-- =========================
-- C. UNIQUE constraints (idempotent create)
-- =========================
SET @has_unique_user_setting :=
    (SELECT COUNT(*)
     FROM information_schema.statistics
     WHERE table_schema = DATABASE()
       AND table_name = 'user_setting'
       AND column_name = 'user_id'
       AND non_unique = 0);
SET @sql := IF(@has_unique_user_setting = 0,
               'CREATE UNIQUE INDEX ux_user_setting_user_id ON user_setting (user_id)',
               'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_unique_user_memory :=
    (SELECT COUNT(*)
     FROM information_schema.statistics
     WHERE table_schema = DATABASE()
       AND table_name = 'user_memory'
       AND column_name = 'user_id'
       AND non_unique = 0);
SET @sql := IF(@has_unique_user_memory = 0,
               'CREATE UNIQUE INDEX ux_user_memory_user_id ON user_memory (user_id)',
               'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- =========================
-- D. FK constraints normalization
-- Goal: enforce ON DELETE CASCADE for user_setting/user_memory -> user
-- =========================
SET @fk_name :=
    (SELECT kcu.CONSTRAINT_NAME
     FROM information_schema.key_column_usage kcu
     WHERE kcu.table_schema = DATABASE()
       AND kcu.table_name = 'user_setting'
       AND kcu.column_name = 'user_id'
       AND kcu.referenced_table_name = 'user'
     LIMIT 1);

SET @fk_delete_rule :=
    (SELECT rc.DELETE_RULE
     FROM information_schema.referential_constraints rc
     WHERE rc.constraint_schema = DATABASE()
       AND rc.table_name = 'user_setting'
       AND rc.constraint_name = @fk_name
     LIMIT 1);

SET @need_recreate_fk := IF(@fk_name IS NULL, 1, IF(@fk_delete_rule = 'CASCADE', 0, 1));

SET @sql := IF(@need_recreate_fk = 1 AND @fk_name IS NOT NULL,
               CONCAT('ALTER TABLE user_setting DROP FOREIGN KEY `', @fk_name, '`'),
               'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(@need_recreate_fk = 1,
               'ALTER TABLE user_setting ADD CONSTRAINT fk_user_setting_user FOREIGN KEY (user_id) REFERENCES `user`(id) ON DELETE CASCADE ON UPDATE RESTRICT',
               'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @fk_name :=
    (SELECT kcu.CONSTRAINT_NAME
     FROM information_schema.key_column_usage kcu
     WHERE kcu.table_schema = DATABASE()
       AND kcu.table_name = 'user_memory'
       AND kcu.column_name = 'user_id'
       AND kcu.referenced_table_name = 'user'
     LIMIT 1);

SET @fk_delete_rule :=
    (SELECT rc.DELETE_RULE
     FROM information_schema.referential_constraints rc
     WHERE rc.constraint_schema = DATABASE()
       AND rc.table_name = 'user_memory'
       AND rc.constraint_name = @fk_name
     LIMIT 1);

SET @need_recreate_fk := IF(@fk_name IS NULL, 1, IF(@fk_delete_rule = 'CASCADE', 0, 1));

SET @sql := IF(@need_recreate_fk = 1 AND @fk_name IS NOT NULL,
               CONCAT('ALTER TABLE user_memory DROP FOREIGN KEY `', @fk_name, '`'),
               'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(@need_recreate_fk = 1,
               'ALTER TABLE user_memory ADD CONSTRAINT fk_user_memory_user FOREIGN KEY (user_id) REFERENCES `user`(id) ON DELETE CASCADE ON UPDATE RESTRICT',
               'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- =========================
-- E. CHECK constraints for date fragments
-- =========================
SET @has_chk_thread_month :=
    (SELECT COUNT(*)
     FROM information_schema.table_constraints
     WHERE table_schema = DATABASE()
       AND table_name = 'thread'
       AND constraint_name = 'chk_thread_month'
       AND constraint_type = 'CHECK');
SET @sql := IF(@has_chk_thread_month = 0,
               'ALTER TABLE thread ADD CONSTRAINT chk_thread_month CHECK (month BETWEEN 1 AND 12)',
               'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_chk_thread_day :=
    (SELECT COUNT(*)
     FROM information_schema.table_constraints
     WHERE table_schema = DATABASE()
       AND table_name = 'thread'
       AND constraint_name = 'chk_thread_day'
       AND constraint_type = 'CHECK');
SET @sql := IF(@has_chk_thread_day = 0,
               'ALTER TABLE thread ADD CONSTRAINT chk_thread_day CHECK (day BETWEEN 1 AND 31)',
               'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_chk_diary_month :=
    (SELECT COUNT(*)
     FROM information_schema.table_constraints
     WHERE table_schema = DATABASE()
       AND table_name = 'diary'
       AND constraint_name = 'chk_diary_month'
       AND constraint_type = 'CHECK');
SET @sql := IF(@has_chk_diary_month = 0,
               'ALTER TABLE diary ADD CONSTRAINT chk_diary_month CHECK (month BETWEEN 1 AND 12)',
               'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_chk_diary_day :=
    (SELECT COUNT(*)
     FROM information_schema.table_constraints
     WHERE table_schema = DATABASE()
       AND table_name = 'diary'
       AND constraint_name = 'chk_diary_day'
       AND constraint_type = 'CHECK');
SET @sql := IF(@has_chk_diary_day = 0,
               'ALTER TABLE diary ADD CONSTRAINT chk_diary_day CHECK (day BETWEEN 1 AND 31)',
               'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- =========================
-- F. Supporting indexes for query patterns
-- =========================
SET @has_idx := (SELECT COUNT(*) FROM information_schema.statistics
                 WHERE table_schema = DATABASE()
                   AND table_name = 'user_setting'
                   AND index_name = 'idx_user_setting_notification_target');
SET @sql := IF(@has_idx = 0,
               'CREATE INDEX idx_user_setting_notification_target ON user_setting (notification_time, notification_enabled, last_sent_date, user_id)',
               'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_idx := (SELECT COUNT(*) FROM information_schema.statistics
                 WHERE table_schema = DATABASE()
                   AND table_name = 'diary'
                   AND index_name = 'idx_diary_user_day_active_created');
SET @sql := IF(@has_idx = 0,
               'CREATE INDEX idx_diary_user_day_active_created ON diary (user_id, year, month, day, is_active, created_at, id)',
               'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_idx := (SELECT COUNT(*) FROM information_schema.statistics
                 WHERE table_schema = DATABASE()
                   AND table_name = 'diary'
                   AND index_name = 'idx_diary_user_month_active_day_created');
SET @sql := IF(@has_idx = 0,
               'CREATE INDEX idx_diary_user_month_active_day_created ON diary (user_id, year, month, is_active, day, created_at, id)',
               'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_idx := (SELECT COUNT(*) FROM information_schema.statistics
                 WHERE table_schema = DATABASE()
                   AND table_name = 'diary'
                   AND index_name = 'idx_diary_feed_user_active_id');
SET @sql := IF(@has_idx = 0,
               'CREATE INDEX idx_diary_feed_user_active_id ON diary (user_id, is_active, id)',
               'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_idx := (SELECT COUNT(*) FROM information_schema.statistics
                 WHERE table_schema = DATABASE()
                   AND table_name = 'diary'
                   AND index_name = 'idx_diary_thread_active_created');
SET @sql := IF(@has_idx = 0,
               'CREATE INDEX idx_diary_thread_active_created ON diary (thread_id, is_active, created_at, id)',
               'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_idx := (SELECT COUNT(*) FROM information_schema.statistics
                 WHERE table_schema = DATABASE()
                   AND table_name = 'daily_chat_log'
                   AND index_name = 'idx_daily_chat_log_thread_created');
SET @sql := IF(@has_idx = 0,
               'CREATE INDEX idx_daily_chat_log_thread_created ON daily_chat_log (thread_id, created_at, id)',
               'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
