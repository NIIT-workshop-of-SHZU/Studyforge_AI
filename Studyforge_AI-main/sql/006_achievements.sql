-- Achievement system and user growth profile
-- Creates tables for achievements, user achievements, and user skill stats

USE studyforge_ai;

-- Achievements definition table
CREATE TABLE IF NOT EXISTS achievements (
    achievement_id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    achievement_code VARCHAR(80) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(300) NOT NULL,
    icon VARCHAR(50) NULL,
    category VARCHAR(30) NOT NULL DEFAULT 'GENERAL',
    requirement_type VARCHAR(50) NOT NULL,
    requirement_value INT UNSIGNED NOT NULL DEFAULT 0,
    reward_points INT UNSIGNED NOT NULL DEFAULT 0,
    sort_no INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_achievements_code (achievement_code),
    KEY idx_achievements_category (category, sort_no)
) ENGINE=InnoDB COMMENT='Achievement definitions';

-- User achievements table
CREATE TABLE IF NOT EXISTS user_achievements (
    id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    achievement_id BIGINT UNSIGNED NOT NULL,
    unlocked_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_achievements (user_id, achievement_id),
    KEY idx_user_achievements_user (user_id, unlocked_time DESC),
    CONSTRAINT fk_user_achievements_user_id FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE,
    CONSTRAINT fk_user_achievements_achievement_id FOREIGN KEY (achievement_id) REFERENCES achievements (achievement_id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='User unlocked achievements';

-- User skill stats table
CREATE TABLE IF NOT EXISTS user_skill_stats (
    id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    skill_id BIGINT UNSIGNED NOT NULL,
    learning_minutes INT UNSIGNED NOT NULL DEFAULT 0,
    posts_read INT UNSIGNED NOT NULL DEFAULT 0,
    posts_published INT UNSIGNED NOT NULL DEFAULT 0,
    roadmaps_completed INT UNSIGNED NOT NULL DEFAULT 0,
    last_activity_time DATETIME NULL,
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_skill_stats (user_id, skill_id),
    KEY idx_user_skill_stats_user (user_id),
    CONSTRAINT fk_user_skill_stats_user_id FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE,
    CONSTRAINT fk_user_skill_stats_skill_id FOREIGN KEY (skill_id) REFERENCES skill_topics (skill_id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='User learning statistics per skill';

-- Seed achievements data
INSERT INTO achievements (achievement_code, name, description, icon, category, requirement_type, requirement_value, reward_points, sort_no) VALUES
('first_login', '初入熔炉', '注册并登录 StudyForge 账号', '🔥', 'GENERAL', 'LOGIN', 1, 10, 1),
('first_post', '第一篇攻略', '发布你的第一篇文章', '📝', 'CONTENT', 'POST_COUNT', 1, 20, 2),
('first_question', '第一次求助', '发布你的第一个求助', '❓', 'CONTENT', 'QUESTION_COUNT', 1, 15, 3),
('first_card', '第一张卡片', '生成你的第一张 AI 复习卡片', '🃏', 'AI', 'CARD_COUNT', 1, 15, 4),
('seven_day_streak', '七日连炉', '连续 7 天有学习行为', '🔥', 'STREAK', 'LOGIN_STREAK', 7, 50, 5),
('deep_work', '深度工作', '单次专注超过 90 分钟', '⏰', 'FOCUS', 'FOCUS_SESSION', 90, 30, 6),
('code_practitioner', '代码修行者', 'IDE 累计使用 20 小时', '💻', 'FOCUS', 'CODING_HOURS', 1200, 40, 7),
('reading_alchemist', '阅读炼金术士', '阅读类应用累计 10 小时', '📖', 'FOCUS', 'READING_HOURS', 600, 35, 8),
('hundred_likes', '百赞作者', '累计获得 100 个赞', '❤️', 'SOCIAL', 'TOTAL_LIKES', 100, 60, 9),
('collector', '收藏家', '收藏 30 篇文章', '📚', 'SOCIAL', 'FAVORITE_COUNT', 30, 30, 10),
('answerer', '解答者', '回答 10 个求助', '💡', 'SOCIAL', 'ANSWER_COUNT', 10, 40, 11),
('rescue_expert', '救援专家', '5 个回答被采纳', '🏆', 'SOCIAL', 'ACCEPTED_ANSWERS', 5, 50, 12),
('roadmap_completer', '路线完成者', '完成一条学习路线', '🗺️', 'LEARNING', 'ROADMAP_COMPLETED', 1, 40, 13),
('reviewer', '复盘者', '连续 7 天生成学习战报', '📊', 'STREAK', 'REVIEW_STREAK', 7, 45, 14),
('ten_posts', '活跃作者', '发布 10 篇文章', '✍️', 'CONTENT', 'POST_COUNT', 10, 35, 15),
('fifty_posts', '内容贡献者', '发布 50 篇文章', '📝', 'CONTENT', 'POST_COUNT', 50, 80, 16),
('thousand_views', '千阅作者', '文章累计被阅读 1000 次', '👁️', 'SOCIAL', 'TOTAL_VIEWS', 1000, 50, 17),
('skill_master', '技能大师', '完成 5 条学习路线', '🎓', 'LEARNING', 'ROADMAP_COMPLETED', 5, 100, 18)
ON DUPLICATE KEY UPDATE name = VALUES(name);
