-- TERM-001 pre-check
-- Target DB: MySQL 8.x
-- Purpose: inspect terms agreement migration readiness

SELECT table_name, table_rows
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name IN ('term', 'term_version', 'user_term_agreement')
ORDER BY table_name;

SELECT table_name, index_name, column_name, seq_in_index, non_unique
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND table_name IN ('term', 'term_version', 'user_term_agreement')
ORDER BY table_name, index_name, seq_in_index;

SET @has_term := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'term'
);

SET @sql := IF(
    @has_term = 1,
    'SELECT term_code, category, display_order, active FROM term ORDER BY display_order',
    'SELECT ''term table not found'' AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_term_version := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'term_version'
);

SET @sql := IF(
    @has_term_version = 1,
    'SELECT t.term_code, tv.version_label, tv.title, tv.required, tv.status, tv.requires_reconsent, tv.effective_from
     FROM term_version tv
     JOIN term t ON t.id = tv.term_id
     ORDER BY t.display_order, tv.effective_from, tv.id',
    'SELECT ''term_version table not found'' AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
