-- Friend chat media: message types and user stickers

SET @alter_sql = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE friend_messages ADD COLUMN message_type VARCHAR(20) NOT NULL DEFAULT ''TEXT'' AFTER content',
    'DO 0')
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'friend_messages' AND COLUMN_NAME = 'message_type');
PREPARE stmt FROM @alter_sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS user_stickers (
    sticker_id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    image_url VARCHAR(512) NOT NULL,
    sort_no INT NOT NULL DEFAULT 0,
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_user_stickers_user_sort (user_id, sort_no, sticker_id),
    CONSTRAINT fk_user_stickers_user_id FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='User saved chat stickers';
