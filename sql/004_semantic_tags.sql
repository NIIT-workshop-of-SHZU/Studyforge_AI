-- Semantic tag cache for LLM-based importance scoring
-- mysql -u studyforge -p studyforge_ai < sql/004_semantic_tags.sql

SET @studyforge_add_column_sql = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE `post_i18n` ADD COLUMN `semantic_tags_json` JSON NULL COMMENT ''LLM semantic tags with weights'' AFTER `ai_tags`', 'DO 0') FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'post_i18n' AND COLUMN_NAME = 'semantic_tags_json');
PREPARE studyforge_add_column_stmt FROM @studyforge_add_column_sql; EXECUTE studyforge_add_column_stmt; DEALLOCATE PREPARE studyforge_add_column_stmt;

SET @studyforge_add_column_sql = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE `post_i18n` ADD COLUMN `semantic_tags_fingerprint` VARCHAR(64) NULL COMMENT ''Hash of title+summary for cache invalidation'' AFTER `semantic_tags_json`', 'DO 0') FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'post_i18n' AND COLUMN_NAME = 'semantic_tags_fingerprint');
PREPARE studyforge_add_column_stmt FROM @studyforge_add_column_sql; EXECUTE studyforge_add_column_stmt; DEALLOCATE PREPARE studyforge_add_column_stmt;
