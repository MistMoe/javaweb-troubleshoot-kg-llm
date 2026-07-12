DROP TABLE IF EXISTS sys_user_role;
DROP TABLE IF EXISTS error_case_favorite;
DROP TABLE IF EXISTS error_case_submission;
DROP TABLE IF EXISTS analysis_history;
DROP TABLE IF EXISTS error_case;
DROP TABLE IF EXISTS sys_role;
DROP TABLE IF EXISTS sys_user;

CREATE TABLE sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    nickname VARCHAR(50),
    email VARCHAR(100),
    status TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE sys_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_code VARCHAR(50) NOT NULL UNIQUE,
    role_name VARCHAR(50) NOT NULL,
    description VARCHAR(255),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE sys_user_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL
);

CREATE TABLE error_case (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    error_name VARCHAR(255) NOT NULL,
    stage VARCHAR(100),
    component VARCHAR(100),
    keyword VARCHAR(500),
    symptom TEXT,
    cause TEXT,
    solution TEXT,
    related_file VARCHAR(255),
    related_command VARCHAR(500),
    risk_tip TEXT,
    create_user_id BIGINT,
    update_user_id BIGINT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE analysis_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT,
    input_text TEXT NOT NULL,
    tech_stack VARCHAR(255),
    matched_error_id BIGINT,
    matched_error_name VARCHAR(255),
    graph_result TEXT,
    ai_advice TEXT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE error_case_submission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    submit_user_id BIGINT,
    raw_log TEXT NOT NULL,
    tech_stack VARCHAR(100),
    stage VARCHAR(100),
    user_solution TEXT,
    related_file VARCHAR(255),
    related_command VARCHAR(500),
    draft_error_name VARCHAR(255) NOT NULL,
    draft_keyword VARCHAR(500) NOT NULL,
    draft_symptom TEXT,
    draft_cause TEXT,
    draft_solution TEXT,
    draft_related_file VARCHAR(255),
    draft_related_command VARCHAR(500),
    draft_risk_tip TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    reviewer_id BIGINT,
    review_comment TEXT,
    approved_error_case_id BIGINT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE error_case_favorite (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    error_case_id BIGINT NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (user_id, error_case_id)
);
