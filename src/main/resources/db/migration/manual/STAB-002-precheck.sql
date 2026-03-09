    -- STAB-002 pre-check (run before apply script)
-- Target DB: MySQL 8.x

-- 1) Null checks
SELECT COUNT(*) AS user_setting_null_user_id
FROM user_setting
WHERE user_id IS NULL;

SELECT COUNT(*) AS user_memory_null_user_id
FROM user_memory
WHERE user_id IS NULL;

-- 2) Duplicate checks for 1:1 tables
SELECT user_id, COUNT(*) AS duplicate_count
FROM user_setting
GROUP BY user_id
HAVING COUNT(*) > 1;

SELECT user_id, COUNT(*) AS duplicate_count
FROM user_memory
GROUP BY user_id
HAVING COUNT(*) > 1;

-- 3) Orphan checks against user table
SELECT COUNT(*) AS user_setting_orphan_count
FROM user_setting s
LEFT JOIN `user` u ON u.id = s.user_id
WHERE s.user_id IS NOT NULL
  AND u.id IS NULL;

SELECT COUNT(*) AS user_memory_orphan_count
FROM user_memory m
LEFT JOIN `user` u ON u.id = m.user_id
WHERE m.user_id IS NOT NULL
  AND u.id IS NULL;

-- 4) Date range checks (for CHECK constraints)
SELECT COUNT(*) AS thread_invalid_month_or_day
FROM thread
WHERE month < 1 OR month > 12 OR day < 1 OR day > 31;

SELECT COUNT(*) AS diary_invalid_month_or_day
FROM diary
WHERE month < 1 OR month > 12 OR day < 1 OR day > 31;

-- 5) Existing FK/unique/index visibility (for review)
SELECT table_name, constraint_name, constraint_type
FROM information_schema.table_constraints
WHERE table_schema = DATABASE()
  AND table_name IN ('user_setting', 'user_memory', 'thread', 'diary', 'daily_chat_log')
ORDER BY table_name, constraint_type, constraint_name;

-- 6) FK action rules for target 1:1 tables
SELECT
  rc.constraint_name,
  rc.table_name,
  rc.referenced_table_name,
  rc.update_rule,
  rc.delete_rule
FROM information_schema.referential_constraints rc
WHERE rc.constraint_schema = DATABASE()
  AND rc.table_name IN ('user_setting', 'user_memory');

-- 7) Nullability check on target FK columns
SELECT table_name, column_name, is_nullable
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name IN ('user_setting', 'user_memory')
  AND column_name = 'user_id';
