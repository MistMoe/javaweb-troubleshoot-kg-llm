CREATE TABLE IF NOT EXISTS error_case_favorite (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT 'favorite owner user id',
    error_case_id BIGINT NOT NULL COMMENT 'favorited error_case id',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_favorite_user_case (user_id, error_case_id),
    KEY idx_favorite_user (user_id),
    KEY idx_favorite_case (error_case_id)
) COMMENT='user favorite error case relation table';
