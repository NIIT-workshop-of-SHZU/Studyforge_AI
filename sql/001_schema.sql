CREATE DATABASE IF NOT EXISTS studyforge_ai
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE studyforge_ai;

CREATE TABLE IF NOT EXISTS users (
    user_id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    reputation_score INT NOT NULL DEFAULT 0,
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_users_username (username),
    UNIQUE KEY uk_users_email (email),
    KEY idx_users_role_status (role, status)
) ENGINE=InnoDB COMMENT='User account and profile';

CREATE TABLE IF NOT EXISTS user_tokens (
    token_id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    access_token VARCHAR(512) NOT NULL,
    expire_time DATETIME NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_tokens_access_token (access_token),
    KEY idx_user_tokens_user_status (user_id, status),
    CONSTRAINT fk_user_tokens_user_id FOREIGN KEY (user_id) REFERENCES users (user_id)
) ENGINE=InnoDB COMMENT='Access token store';

CREATE TABLE IF NOT EXISTS integration_settings (
    setting_key VARCHAR(80) PRIMARY KEY,
    setting_value TEXT NULL,
    secret_flag TINYINT(1) NOT NULL DEFAULT 0,
    updated_by BIGINT UNSIGNED NULL,
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_integration_settings_secret (secret_flag),
    CONSTRAINT fk_integration_settings_updated_by FOREIGN KEY (updated_by) REFERENCES users (user_id) ON DELETE SET NULL
) ENGINE=InnoDB COMMENT='External AI and voice integration settings';

CREATE TABLE IF NOT EXISTS categories (
    category_id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    category_code VARCHAR(50) NOT NULL,
    sort_no INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_categories_code (category_code),
    KEY idx_categories_status_sort (status, sort_no)
) ENGINE=InnoDB COMMENT='Category dictionary';

CREATE TABLE IF NOT EXISTS category_i18n (
    id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    category_id BIGINT UNSIGNED NOT NULL,
    language_code VARCHAR(16) NOT NULL,
    name VARCHAR(100) NOT NULL,
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_category_i18n_category_lang (category_id, language_code),
    KEY idx_category_i18n_lang_name (language_code, name),
    CONSTRAINT fk_category_i18n_category_id FOREIGN KEY (category_id) REFERENCES categories (category_id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='Localized category names';

CREATE TABLE IF NOT EXISTS posts (
    post_id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    author_id BIGINT UNSIGNED NOT NULL,
    category_id BIGINT UNSIGNED NOT NULL,
    original_language VARCHAR(16) NOT NULL DEFAULT 'zh_CN',
    status VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED',
    cover_image_url VARCHAR(512) NULL,
    featured TINYINT(1) NOT NULL DEFAULT 0,
    like_count INT UNSIGNED NOT NULL DEFAULT 0,
    favorite_count INT UNSIGNED NOT NULL DEFAULT 0,
    comment_count INT UNSIGNED NOT NULL DEFAULT 0,
    view_count INT UNSIGNED NOT NULL DEFAULT 0,
    hot_score DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_posts_author_id (author_id),
    KEY idx_posts_category_id (category_id),
    KEY idx_posts_status_featured (status, featured),
    KEY idx_posts_hot_score (hot_score DESC),
    CONSTRAINT fk_posts_author_id FOREIGN KEY (author_id) REFERENCES users (user_id),
    CONSTRAINT fk_posts_category_id FOREIGN KEY (category_id) REFERENCES categories (category_id)
) ENGINE=InnoDB COMMENT='Post aggregate root';

ALTER TABLE posts
    ADD COLUMN IF NOT EXISTS cover_image_url VARCHAR(512) NULL AFTER status;

CREATE TABLE IF NOT EXISTS post_i18n (
    id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    post_id BIGINT UNSIGNED NOT NULL,
    language_code VARCHAR(16) NOT NULL,
    title VARCHAR(200) NOT NULL,
    summary TEXT NULL,
    content MEDIUMTEXT NOT NULL,
    content_format VARCHAR(20) NOT NULL DEFAULT 'MARKDOWN',
    ai_tags VARCHAR(500) NULL,
    source_type VARCHAR(20) NOT NULL DEFAULT 'ORIGINAL',
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_post_i18n_post_lang (post_id, language_code),
    KEY idx_post_i18n_lang_post (language_code, post_id),
    FULLTEXT KEY ft_post_i18n_search (title, summary, content),
    CONSTRAINT fk_post_i18n_post_id FOREIGN KEY (post_id) REFERENCES posts (post_id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='Localized post content';

ALTER TABLE post_i18n
    ADD COLUMN IF NOT EXISTS content_format VARCHAR(20) NOT NULL DEFAULT 'MARKDOWN' AFTER content;

CREATE TABLE IF NOT EXISTS uploaded_files (
    file_id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NULL,
    biz_type VARCHAR(30) NOT NULL DEFAULT 'POST_IMAGE',
    original_filename VARCHAR(255) NOT NULL,
    stored_filename VARCHAR(255) NOT NULL,
    file_url VARCHAR(512) NOT NULL,
    content_type VARCHAR(100) NULL,
    file_size BIGINT UNSIGNED NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_uploaded_files_stored_filename (stored_filename),
    KEY idx_uploaded_files_user_created (user_id, created_time DESC),
    KEY idx_uploaded_files_biz_status (biz_type, status),
    CONSTRAINT fk_uploaded_files_user_id FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE SET NULL
) ENGINE=InnoDB COMMENT='Uploaded image and attachment metadata';

CREATE TABLE IF NOT EXISTS comments (
    comment_id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    post_id BIGINT UNSIGNED NOT NULL,
    user_id BIGINT UNSIGNED NOT NULL,
    language_code VARCHAR(16) NOT NULL DEFAULT 'zh_CN',
    content TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'VISIBLE',
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_comments_post_created (post_id, created_time DESC),
    KEY idx_comments_user_created (user_id, created_time DESC),
    CONSTRAINT fk_comments_post_id FOREIGN KEY (post_id) REFERENCES posts (post_id) ON DELETE CASCADE,
    CONSTRAINT fk_comments_user_id FOREIGN KEY (user_id) REFERENCES users (user_id)
) ENGINE=InnoDB COMMENT='Post comments';

CREATE TABLE IF NOT EXISTS post_likes (
    like_id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    post_id BIGINT UNSIGNED NOT NULL,
    user_id BIGINT UNSIGNED NOT NULL,
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_post_likes_post_user (post_id, user_id),
    KEY idx_post_likes_user_created (user_id, created_time DESC),
    CONSTRAINT fk_post_likes_post_id FOREIGN KEY (post_id) REFERENCES posts (post_id) ON DELETE CASCADE,
    CONSTRAINT fk_post_likes_user_id FOREIGN KEY (user_id) REFERENCES users (user_id)
) ENGINE=InnoDB COMMENT='Post likes';

CREATE TABLE IF NOT EXISTS post_favorites (
    favorite_id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    post_id BIGINT UNSIGNED NOT NULL,
    user_id BIGINT UNSIGNED NOT NULL,
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_post_favorites_post_user (post_id, user_id),
    KEY idx_post_favorites_user_created (user_id, created_time DESC),
    CONSTRAINT fk_post_favorites_post_id FOREIGN KEY (post_id) REFERENCES posts (post_id) ON DELETE CASCADE,
    CONSTRAINT fk_post_favorites_user_id FOREIGN KEY (user_id) REFERENCES users (user_id)
) ENGINE=InnoDB COMMENT='Post favorites';

CREATE TABLE IF NOT EXISTS post_view_history (
    id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    post_id BIGINT UNSIGNED NOT NULL,
    user_id BIGINT UNSIGNED NOT NULL,
    view_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_post_view_history_user_time (user_id, view_time DESC),
    KEY idx_post_view_history_post_time (post_id, view_time DESC),
    CONSTRAINT fk_post_view_history_post_id FOREIGN KEY (post_id) REFERENCES posts (post_id) ON DELETE CASCADE,
    CONSTRAINT fk_post_view_history_user_id FOREIGN KEY (user_id) REFERENCES users (user_id)
) ENGINE=InnoDB COMMENT='Recent browsing history';

CREATE TABLE IF NOT EXISTS reports (
    report_id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    post_id BIGINT UNSIGNED NOT NULL,
    reporter_id BIGINT UNSIGNED NOT NULL,
    reason VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    ai_risk_level VARCHAR(20) NULL,
    ai_suggestion TEXT NULL,
    processed_by BIGINT UNSIGNED NULL,
    processed_time DATETIME NULL,
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_reports_status_created (status, created_time DESC),
    KEY idx_reports_post_created (post_id, created_time DESC),
    CONSTRAINT fk_reports_post_id FOREIGN KEY (post_id) REFERENCES posts (post_id) ON DELETE CASCADE,
    CONSTRAINT fk_reports_reporter_id FOREIGN KEY (reporter_id) REFERENCES users (user_id),
    CONSTRAINT fk_reports_processed_by FOREIGN KEY (processed_by) REFERENCES users (user_id)
) ENGINE=InnoDB COMMENT='Report and moderation queue';

CREATE TABLE IF NOT EXISTS ai_logs (
    log_id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NULL,
    post_id BIGINT UNSIGNED NULL,
    ai_type VARCHAR(50) NOT NULL,
    request_text MEDIUMTEXT NULL,
    response_text MEDIUMTEXT NULL,
    success TINYINT(1) NOT NULL DEFAULT 1,
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_ai_logs_type_created (ai_type, created_time DESC),
    KEY idx_ai_logs_user_created (user_id, created_time DESC),
    CONSTRAINT fk_ai_logs_user_id FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT fk_ai_logs_post_id FOREIGN KEY (post_id) REFERENCES posts (post_id) ON DELETE SET NULL
) ENGINE=InnoDB COMMENT='AI call audit log';

CREATE TABLE IF NOT EXISTS voice_records (
    record_id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NULL,
    post_id BIGINT UNSIGNED NULL,
    voice_type VARCHAR(20) NOT NULL,
    audio_url VARCHAR(512) NULL,
    recognized_text TEXT NULL,
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_voice_records_type_created (voice_type, created_time DESC),
    KEY idx_voice_records_user_created (user_id, created_time DESC),
    CONSTRAINT fk_voice_records_user_id FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT fk_voice_records_post_id FOREIGN KEY (post_id) REFERENCES posts (post_id) ON DELETE SET NULL
) ENGINE=InnoDB COMMENT='Voice interaction records';

CREATE TABLE IF NOT EXISTS help_requests (
    help_id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    category_id BIGINT UNSIGNED NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    reward_points INT NOT NULL DEFAULT 0,
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_help_requests_user_created (user_id, created_time DESC),
    KEY idx_help_requests_status_created (status, created_time DESC),
    CONSTRAINT fk_help_requests_user_id FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT fk_help_requests_category_id FOREIGN KEY (category_id) REFERENCES categories (category_id)
) ENGINE=InnoDB COMMENT='Study help requests';

CREATE TABLE IF NOT EXISTS help_answers (
    answer_id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    help_id BIGINT UNSIGNED NOT NULL,
    user_id BIGINT UNSIGNED NOT NULL,
    content TEXT NOT NULL,
    is_accepted TINYINT(1) NOT NULL DEFAULT 0,
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_help_answers_help_created (help_id, created_time DESC),
    KEY idx_help_answers_user_created (user_id, created_time DESC),
    CONSTRAINT fk_help_answers_help_id FOREIGN KEY (help_id) REFERENCES help_requests (help_id) ON DELETE CASCADE,
    CONSTRAINT fk_help_answers_user_id FOREIGN KEY (user_id) REFERENCES users (user_id)
) ENGINE=InnoDB COMMENT='Answers for help requests';

CREATE TABLE IF NOT EXISTS admin_audit_logs (
    log_id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    admin_id BIGINT UNSIGNED NOT NULL,
    target_type VARCHAR(50) NOT NULL,
    target_id BIGINT UNSIGNED NOT NULL,
    action_type VARCHAR(50) NOT NULL,
    remark VARCHAR(500) NULL,
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_admin_audit_logs_admin_created (admin_id, created_time DESC),
    KEY idx_admin_audit_logs_target (target_type, target_id),
    CONSTRAINT fk_admin_audit_logs_admin_id FOREIGN KEY (admin_id) REFERENCES users (user_id)
) ENGINE=InnoDB COMMENT='Administrative action log';
