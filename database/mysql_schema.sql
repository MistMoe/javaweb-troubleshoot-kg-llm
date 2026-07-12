CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE COMMENT 'username',
    password VARCHAR(255) NOT NULL COMMENT 'password, should be encrypted in real projects',
    nickname VARCHAR(50) COMMENT 'nickname',
    email VARCHAR(100) COMMENT 'email',
    status TINYINT DEFAULT 1 COMMENT '1 enabled, 0 disabled',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT='system user table';

CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_code VARCHAR(50) NOT NULL UNIQUE COMMENT 'ADMIN, MAINTAINER, USER',
    role_name VARCHAR(50) NOT NULL COMMENT 'role name',
    description VARCHAR(255) COMMENT 'role description',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT='system role table';

CREATE TABLE IF NOT EXISTS sys_user_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT 'user id',
    role_id BIGINT NOT NULL COMMENT 'role id',
    UNIQUE KEY uk_user_role (user_id, role_id)
) COMMENT='user-role relation table';

CREATE TABLE IF NOT EXISTS error_case (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    error_name VARCHAR(255) NOT NULL COMMENT 'error name',
    stage VARCHAR(100) COMMENT 'stage',
    component VARCHAR(100) COMMENT 'component',
    keyword VARCHAR(500) COMMENT 'keywords for matching',
    symptom TEXT COMMENT 'symptom',
    cause TEXT COMMENT 'possible cause',
    solution TEXT COMMENT 'solution',
    related_file VARCHAR(255) COMMENT 'related file',
    related_command VARCHAR(500) COMMENT 'related command',
    risk_tip TEXT COMMENT 'risk tip',
    create_user_id BIGINT COMMENT 'creator id',
    update_user_id BIGINT COMMENT 'updater id',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_error_case_name (error_name)
) COMMENT='error knowledge base table';

CREATE TABLE IF NOT EXISTS analysis_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT COMMENT 'submitter user id',
    input_text TEXT NOT NULL COMMENT 'original error text',
    tech_stack VARCHAR(255) COMMENT 'selected tech stack',
    matched_error_id BIGINT COMMENT 'matched error case id',
    matched_error_name VARCHAR(255) COMMENT 'matched error name',
    graph_result JSON COMMENT 'Neo4j retrieval result',
    ai_advice TEXT COMMENT 'LLM or knowledge based advice',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT='analysis history table';

CREATE TABLE IF NOT EXISTS error_case_submission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    submit_user_id BIGINT COMMENT 'submitter user id',
    raw_log TEXT NOT NULL COMMENT 'raw error log or troubleshooting context',
    tech_stack VARCHAR(100) COMMENT 'technology stack',
    stage VARCHAR(100) COMMENT 'failure stage',
    user_solution TEXT COMMENT 'submitter solution experience',
    related_file VARCHAR(255) COMMENT 'related config or source file',
    related_command VARCHAR(500) COMMENT 'related troubleshooting command',
    draft_error_name VARCHAR(255) NOT NULL COMMENT 'AI or user drafted case title',
    draft_keyword VARCHAR(500) NOT NULL COMMENT 'AI or user drafted matching keywords',
    draft_symptom TEXT COMMENT 'draft symptom',
    draft_cause TEXT COMMENT 'draft cause',
    draft_solution TEXT COMMENT 'draft solution',
    draft_related_file VARCHAR(255) COMMENT 'draft related file',
    draft_related_command VARCHAR(500) COMMENT 'draft related command',
    draft_risk_tip TEXT COMMENT 'draft risk tip',
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING, APPROVED, REJECTED',
    reviewer_id BIGINT COMMENT 'reviewer user id',
    review_comment TEXT COMMENT 'review comment',
    approved_error_case_id BIGINT COMMENT 'created error_case id after approval',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_submission_status (status),
    KEY idx_submission_user (submit_user_id),
    KEY idx_submission_approved_case (approved_error_case_id)
) COMMENT='user submitted troubleshooting experience review table';

CREATE TABLE IF NOT EXISTS error_case_favorite (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT 'favorite owner user id',
    error_case_id BIGINT NOT NULL COMMENT 'favorited error_case id',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_favorite_user_case (user_id, error_case_id),
    KEY idx_favorite_user (user_id),
    KEY idx_favorite_case (error_case_id)
) COMMENT='user favorite error case relation table';
