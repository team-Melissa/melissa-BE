-- STAB-007 pre-check
-- Target DB: MySQL 8.x
-- Purpose: inspect async job/outbox migration readiness

SELECT COUNT(*) AS has_async_job_table
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name = 'async_job';

SELECT COUNT(*) AS has_outbox_event_table
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name = 'outbox_event';

SELECT table_name, index_name, column_name, seq_in_index
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND table_name IN ('async_job', 'outbox_event')
ORDER BY table_name, index_name, seq_in_index;
