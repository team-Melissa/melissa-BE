-- STAB-006 pre-check
-- Target DB: MySQL 8.x
-- Purpose: inspect idempotency_record migration readiness

SELECT COUNT(*) AS has_idempotency_record_table
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name = 'idempotency_record';

SELECT table_name, index_name, column_name, seq_in_index
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND table_name = 'idempotency_record'
ORDER BY index_name, seq_in_index;
