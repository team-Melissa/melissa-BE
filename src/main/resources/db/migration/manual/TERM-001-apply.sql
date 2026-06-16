-- TERM-001 apply script
-- Target DB: MySQL 8.x
-- Purpose: add terms version management and user agreement history

-- =========================
-- A. term
-- =========================
SET @has_term := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'term'
);

SET @sql := IF(
    @has_term = 0,
    'CREATE TABLE term (
        id BIGINT NOT NULL AUTO_INCREMENT,
        term_code VARCHAR(50) NOT NULL,
        category VARCHAR(50) NOT NULL,
        display_order INT NOT NULL,
        active BOOLEAN NOT NULL DEFAULT TRUE,
        created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
        updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
        version BIGINT NOT NULL DEFAULT 0,
        PRIMARY KEY (id),
        CONSTRAINT uk_term_code UNIQUE (term_code),
        KEY idx_term_active_order (active, display_order)
    )',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

INSERT INTO term (
    term_code,
    category,
    display_order,
    active,
    created_at,
    updated_at
) VALUES
('SERVICE_TERMS', 'SERVICE', 1, TRUE, NOW(6), NOW(6)),
('PRIVACY_POLICY', 'PRIVACY', 2, TRUE, NOW(6), NOW(6)),
('MARKETING', 'MARKETING', 3, TRUE, NOW(6), NOW(6))
ON DUPLICATE KEY UPDATE
    category = VALUES(category),
    display_order = VALUES(display_order),
    active = VALUES(active),
    updated_at = NOW(6);

-- =========================
-- B. term_version
-- =========================
SET @has_term_version := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'term_version'
);

SET @sql := IF(
    @has_term_version = 0,
    'CREATE TABLE term_version (
        id BIGINT NOT NULL AUTO_INCREMENT,
        term_id BIGINT NOT NULL,
        version_label VARCHAR(20) NOT NULL,
        title VARCHAR(100) NOT NULL,
        required BOOLEAN NOT NULL,
        content LONGTEXT NOT NULL,
        content_format VARCHAR(20) NOT NULL DEFAULT ''TEXT'',
        status VARCHAR(20) NOT NULL DEFAULT ''DRAFT'',
        requires_reconsent BOOLEAN NOT NULL DEFAULT TRUE,
        effective_from DATETIME(6) NOT NULL,
        published_at DATETIME(6) NULL,
        created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
        updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
        version BIGINT NOT NULL DEFAULT 0,
        PRIMARY KEY (id),
        CONSTRAINT uk_term_version_term_version UNIQUE (term_id, version_label),
        KEY idx_term_version_current (term_id, status, effective_from),
        CONSTRAINT fk_term_version_term
            FOREIGN KEY (term_id) REFERENCES term(id)
            ON DELETE RESTRICT
    )',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @service_terms_v1 := '서비스 이용약관 v1.0 목업 내용입니다.';
SET @privacy_policy_v1 := '개인정보 처리방침 v1.0 목업 내용입니다.';
SET @marketing_v1 := '마케팅 정보 수신 동의 v1.0 목업 내용입니다.';

INSERT INTO term_version (
    term_id,
    version_label,
    title,
    required,
    content,
    content_format,
    status,
    requires_reconsent,
    effective_from,
    published_at,
    created_at,
    updated_at
)
SELECT
    t.id,
    '1.0',
    '서비스 이용약관',
    TRUE,
    @service_terms_v1,
    'TEXT',
    'PUBLISHED',
    TRUE,
    NOW(6),
    NOW(6),
    NOW(6),
    NOW(6)
FROM term t
WHERE t.term_code = 'SERVICE_TERMS'
  AND NOT EXISTS (
      SELECT 1
      FROM term_version tv
      WHERE tv.term_id = t.id
        AND tv.version_label = '1.0'
  );

INSERT INTO term_version (
    term_id,
    version_label,
    title,
    required,
    content,
    content_format,
    status,
    requires_reconsent,
    effective_from,
    published_at,
    created_at,
    updated_at
)
SELECT
    t.id,
    '1.0',
    '개인정보 처리방침',
    TRUE,
    @privacy_policy_v1,
    'TEXT',
    'PUBLISHED',
    TRUE,
    NOW(6),
    NOW(6),
    NOW(6),
    NOW(6)
FROM term t
WHERE t.term_code = 'PRIVACY_POLICY'
  AND NOT EXISTS (
      SELECT 1
      FROM term_version tv
      WHERE tv.term_id = t.id
        AND tv.version_label = '1.0'
  );

INSERT INTO term_version (
    term_id,
    version_label,
    title,
    required,
    content,
    content_format,
    status,
    requires_reconsent,
    effective_from,
    published_at,
    created_at,
    updated_at
)
SELECT
    t.id,
    '1.0',
    '마케팅 정보 수신 동의',
    FALSE,
    @marketing_v1,
    'TEXT',
    'PUBLISHED',
    FALSE,
    NOW(6),
    NOW(6),
    NOW(6),
    NOW(6)
FROM term t
WHERE t.term_code = 'MARKETING'
  AND NOT EXISTS (
      SELECT 1
      FROM term_version tv
      WHERE tv.term_id = t.id
        AND tv.version_label = '1.0'
  );

-- =========================
-- C. user_term_agreement
-- =========================
SET @has_user_term_agreement := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'user_term_agreement'
);

SET @sql := IF(
    @has_user_term_agreement = 0,
    'CREATE TABLE user_term_agreement (
        id BIGINT NOT NULL AUTO_INCREMENT,
        user_id BIGINT NOT NULL,
        term_id BIGINT NOT NULL,
        term_version_id BIGINT NOT NULL,
        term_code_snapshot VARCHAR(50) NOT NULL,
        version_label_snapshot VARCHAR(20) NOT NULL,
        title_snapshot VARCHAR(100) NOT NULL,
        required_snapshot BOOLEAN NOT NULL,
        agreed BOOLEAN NOT NULL,
        agreement_context VARCHAR(30) NOT NULL,
        decided_at DATETIME(6) NOT NULL,
        created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
        updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
        PRIMARY KEY (id),
        KEY idx_user_term_agreement_user_term_decided (user_id, term_id, decided_at, id),
        KEY idx_user_term_agreement_user_version (user_id, term_version_id),
        KEY idx_user_term_agreement_version (term_version_id),
        CONSTRAINT fk_user_term_agreement_user
            FOREIGN KEY (user_id) REFERENCES `user`(id)
            ON DELETE RESTRICT,
        CONSTRAINT fk_user_term_agreement_term
            FOREIGN KEY (term_id) REFERENCES term(id)
            ON DELETE RESTRICT,
        CONSTRAINT fk_user_term_agreement_term_version
            FOREIGN KEY (term_version_id) REFERENCES term_version(id)
            ON DELETE RESTRICT
    )',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
