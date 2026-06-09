-- Apply learning memory schema (safe to re-run on existing DB)
-- mysql -u studyforge -p studyforge_ai < sql/003_user_learning_memory.sql

SET @studyforge_add_column_sql = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE `favorite_collection_items` ADD COLUMN `importance_score` DECIMAL(8,4) NOT NULL DEFAULT 0.0000 AFTER `created_time`', 'DO 0') FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'favorite_collection_items' AND COLUMN_NAME = 'importance_score');
PREPARE studyforge_add_column_stmt FROM @studyforge_add_column_sql; EXECUTE studyforge_add_column_stmt; DEALLOCATE PREPARE studyforge_add_column_stmt;

SET @studyforge_add_column_sql = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE `favorite_collection_items` ADD COLUMN `pinned` TINYINT(1) NOT NULL DEFAULT 0 AFTER `importance_score`', 'DO 0') FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'favorite_collection_items' AND COLUMN_NAME = 'pinned');
PREPARE studyforge_add_column_stmt FROM @studyforge_add_column_sql; EXECUTE studyforge_add_column_stmt; DEALLOCATE PREPARE studyforge_add_column_stmt;

SET @studyforge_add_column_sql = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE `favorite_collection_items` ADD COLUMN `score_factors` JSON NULL AFTER `pinned`', 'DO 0') FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'favorite_collection_items' AND COLUMN_NAME = 'score_factors');
PREPARE studyforge_add_column_stmt FROM @studyforge_add_column_sql; EXECUTE studyforge_add_column_stmt; DEALLOCATE PREPARE studyforge_add_column_stmt;

SET @studyforge_add_index_sql = (SELECT IF(COUNT(*) = 0, 'CREATE INDEX `idx_favorite_items_collection_importance` ON `favorite_collection_items` (`collection_id`, `pinned` DESC, `importance_score` DESC, `created_time` DESC)', 'DO 0') FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'favorite_collection_items' AND INDEX_NAME = 'idx_favorite_items_collection_importance');
PREPARE studyforge_add_index_stmt FROM @studyforge_add_index_sql; EXECUTE studyforge_add_index_stmt; DEALLOCATE PREPARE studyforge_add_index_stmt;

CREATE TABLE IF NOT EXISTS user_learning_profiles (
    user_id BIGINT UNSIGNED PRIMARY KEY,
    memory_md TEXT NULL,
    interest_tags JSON NULL,
    embedding_json MEDIUMTEXT NULL,
    profile_version INT UNSIGNED NOT NULL DEFAULT 1,
    ai_memory_enabled TINYINT(1) NOT NULL DEFAULT 1,
    last_refreshed_at DATETIME NULL,
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_learning_profiles_user_id FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='Per-user learning memory and interest profile';

SET @studyforge_add_column_sql = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE `user_learning_profiles` ADD COLUMN `memory_manually_edited` TINYINT(1) NOT NULL DEFAULT 0 AFTER `memory_md`', 'DO 0') FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_learning_profiles' AND COLUMN_NAME = 'memory_manually_edited');
PREPARE studyforge_add_column_stmt FROM @studyforge_add_column_sql; EXECUTE studyforge_add_column_stmt; DEALLOCATE PREPARE studyforge_add_column_stmt;
