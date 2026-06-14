-- PAY-001 rollback script
-- Target DB: MySQL 8.x

SET @has_payment_event := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'payment_event'
);

SET @sql := IF(@has_payment_event > 0, 'DROP TABLE payment_event', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_entitlement := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'entitlement'
);

SET @sql := IF(@has_entitlement > 0, 'DROP TABLE entitlement', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_payment := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'payment'
);

SET @sql := IF(@has_payment > 0, 'DROP TABLE payment', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_payment_product := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'payment_product'
);

SET @sql := IF(@has_payment_product > 0, 'DROP TABLE payment_product', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_idx_user_account_role := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'user'
      AND index_name = 'idx_user_account_role'
);

SET @sql := IF(@has_idx_user_account_role > 0, 'DROP INDEX idx_user_account_role ON `user`', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_account_role := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'user'
      AND column_name = 'account_role'
);

SET @sql := IF(@has_account_role > 0, 'ALTER TABLE `user` DROP COLUMN account_role', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
