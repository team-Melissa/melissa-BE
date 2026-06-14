-- PAY-001 pre-check
-- Target DB: MySQL 8.x
-- Purpose: inspect payment foundation migration readiness

SELECT COUNT(*) AS has_user_account_role_column
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'user'
  AND column_name = 'account_role';

SELECT table_name, table_rows
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name IN ('payment_product', 'payment', 'entitlement', 'payment_event')
ORDER BY table_name;

SELECT table_name, index_name, column_name, seq_in_index, non_unique
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND table_name IN ('payment_product', 'payment', 'entitlement', 'payment_event')
ORDER BY table_name, index_name, seq_in_index;

SET @has_payment_product := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'payment_product'
);

SET @sql := IF(
    @has_payment_product = 1,
    'SELECT COUNT(*) AS google_remove_ads_product_count FROM payment_product WHERE platform = ''GOOGLE'' AND store_product_id = ''premium''',
    'SELECT 0 AS google_remove_ads_product_count'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
    @has_payment_product = 1,
    'SELECT COUNT(*) AS apple_remove_ads_product_count FROM payment_product WHERE platform = ''APPLE'' AND store_product_id = ''com.melissa.melissaFE.premium''',
    'SELECT 0 AS apple_remove_ads_product_count'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
