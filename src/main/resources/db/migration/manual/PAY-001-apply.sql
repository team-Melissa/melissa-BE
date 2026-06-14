-- PAY-001 apply script
-- Target DB: MySQL 8.x
-- Purpose: add payment foundation, entitlement lookup, and account-role storage

-- =========================
-- A. user.account_role
-- =========================
SET @has_account_role := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'user'
      AND column_name = 'account_role'
);

SET @sql := IF(
    @has_account_role = 0,
    'ALTER TABLE `user` ADD COLUMN account_role VARCHAR(20) NOT NULL DEFAULT ''USER''',
    'SELECT 1'
);
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

SET @sql := IF(
    @has_idx_user_account_role = 0,
    'CREATE INDEX idx_user_account_role ON `user` (account_role)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- =========================
-- B. payment_product
-- =========================
SET @has_payment_product := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'payment_product'
);

SET @sql := IF(
    @has_payment_product = 0,
    'CREATE TABLE payment_product (
        id BIGINT NOT NULL AUTO_INCREMENT,
        product_id VARCHAR(100) NOT NULL,
        platform VARCHAR(20) NOT NULL,
        store_product_id VARCHAR(150) NOT NULL,
        product_type VARCHAR(30) NOT NULL,
        entitlement_type VARCHAR(50) NULL,
        active BOOLEAN NOT NULL DEFAULT TRUE,
        display_name VARCHAR(100) NOT NULL,
        created_at DATETIME(6) NOT NULL,
        updated_at DATETIME(6) NOT NULL,
        PRIMARY KEY (id),
        UNIQUE KEY uk_payment_product_platform_store_product (platform, store_product_id),
        KEY idx_payment_product_product_id (product_id),
        KEY idx_payment_product_active (active)
    )',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

INSERT INTO payment_product (
    product_id,
    platform,
    store_product_id,
    product_type,
    entitlement_type,
    active,
    display_name,
    created_at,
    updated_at
) VALUES
('remove_ads', 'GOOGLE', 'premium', 'NON_CONSUMABLE', 'REMOVE_ADS', TRUE, '광고 제거', NOW(6), NOW(6)),
('remove_ads', 'APPLE', 'com.melissa.melissaFE.premium', 'NON_CONSUMABLE', 'REMOVE_ADS', TRUE, '광고 제거', NOW(6), NOW(6))
ON DUPLICATE KEY UPDATE
    product_id = VALUES(product_id),
    product_type = VALUES(product_type),
    entitlement_type = VALUES(entitlement_type),
    active = VALUES(active),
    display_name = VALUES(display_name),
    updated_at = NOW(6);

-- =========================
-- C. payment
-- =========================
SET @has_payment := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'payment'
);

SET @sql := IF(
    @has_payment = 0,
    'CREATE TABLE payment (
        id BIGINT NOT NULL AUTO_INCREMENT,
        user_id BIGINT NOT NULL,
        platform VARCHAR(20) NOT NULL,
        product_id BIGINT NOT NULL,
        store_product_id VARCHAR(150) NOT NULL,
        product_type VARCHAR(30) NOT NULL,
        status VARCHAR(30) NOT NULL,
        amount_micros BIGINT NULL,
        currency VARCHAR(10) NULL,
        google_purchase_token_hash CHAR(64) NULL,
        google_purchase_token_encrypted TEXT NULL,
        google_order_id VARCHAR(100) NULL,
        apple_transaction_id VARCHAR(100) NULL,
        apple_original_transaction_id VARCHAR(100) NULL,
        apple_environment VARCHAR(30) NULL,
        purchased_at DATETIME(6) NULL,
        verified_at DATETIME(6) NULL,
        acknowledged_at DATETIME(6) NULL,
        refunded_at DATETIME(6) NULL,
        revoked_at DATETIME(6) NULL,
        raw_verified_payload JSON NULL,
        failure_reason VARCHAR(500) NULL,
        created_at DATETIME(6) NOT NULL,
        updated_at DATETIME(6) NOT NULL,
        version BIGINT NOT NULL DEFAULT 0,
        PRIMARY KEY (id),
        CONSTRAINT fk_payment_user FOREIGN KEY (user_id) REFERENCES `user` (id) ON DELETE RESTRICT,
        CONSTRAINT fk_payment_product FOREIGN KEY (product_id) REFERENCES payment_product (id),
        UNIQUE KEY uk_payment_google_token (platform, google_purchase_token_hash),
        UNIQUE KEY uk_payment_apple_transaction (platform, apple_transaction_id),
        KEY idx_payment_user_status (user_id, status),
        KEY idx_payment_user_product (user_id, product_id),
        KEY idx_payment_google_order (google_order_id),
        KEY idx_payment_apple_original_transaction (apple_original_transaction_id),
        KEY idx_payment_created_at (created_at)
    )',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- =========================
-- D. entitlement
-- =========================
SET @has_entitlement := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'entitlement'
);

SET @sql := IF(
    @has_entitlement = 0,
    'CREATE TABLE entitlement (
        id BIGINT NOT NULL AUTO_INCREMENT,
        user_id BIGINT NOT NULL,
        entitlement_type VARCHAR(50) NOT NULL,
        active BOOLEAN NOT NULL,
        source_type VARCHAR(30) NOT NULL,
        source_platform VARCHAR(20) NULL,
        source_payment_id BIGINT NULL,
        granted_at DATETIME(6) NOT NULL,
        revoked_at DATETIME(6) NULL,
        revocation_reason VARCHAR(100) NULL,
        created_at DATETIME(6) NOT NULL,
        updated_at DATETIME(6) NOT NULL,
        version BIGINT NOT NULL DEFAULT 0,
        PRIMARY KEY (id),
        CONSTRAINT fk_entitlement_user FOREIGN KEY (user_id) REFERENCES `user` (id) ON DELETE RESTRICT,
        CONSTRAINT fk_entitlement_payment FOREIGN KEY (source_payment_id) REFERENCES payment (id),
        UNIQUE KEY uk_entitlement_user_type (user_id, entitlement_type),
        KEY idx_entitlement_active (active),
        KEY idx_entitlement_source_payment (source_payment_id)
    )',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- =========================
-- E. payment_event
-- =========================
SET @has_payment_event := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'payment_event'
);

SET @sql := IF(
    @has_payment_event = 0,
    'CREATE TABLE payment_event (
        id BIGINT NOT NULL AUTO_INCREMENT,
        platform VARCHAR(20) NOT NULL,
        event_id VARCHAR(200) NOT NULL,
        event_type VARCHAR(100) NOT NULL,
        payment_id BIGINT NULL,
        actor_user_id BIGINT NULL,
        google_purchase_token_hash CHAR(64) NULL,
        apple_transaction_id VARCHAR(100) NULL,
        raw_payload JSON NULL,
        received_at DATETIME(6) NOT NULL,
        processed_at DATETIME(6) NULL,
        processing_status VARCHAR(30) NOT NULL,
        last_error VARCHAR(500) NULL,
        PRIMARY KEY (id),
        CONSTRAINT fk_payment_event_payment FOREIGN KEY (payment_id) REFERENCES payment (id),
        CONSTRAINT fk_payment_event_actor_user FOREIGN KEY (actor_user_id) REFERENCES `user` (id) ON DELETE SET NULL,
        UNIQUE KEY uk_payment_event_platform_event (platform, event_id),
        KEY idx_payment_event_processing (processing_status, received_at),
        KEY idx_payment_event_payment (payment_id),
        KEY idx_payment_event_actor_user (actor_user_id)
    )',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Admin account designation example:
-- UPDATE `user` SET account_role = 'ADMIN' WHERE id = ?;
