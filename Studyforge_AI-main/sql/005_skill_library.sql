-- Skill library and learning roadmap system
-- Creates tables for skills, roadmaps, roadmap nodes, and user progress

USE studyforge_ai;

-- Skills/Topics table
CREATE TABLE IF NOT EXISTS skill_topics (
    skill_id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    skill_code VARCHAR(80) NOT NULL,
    cover_image_url VARCHAR(512) NULL,
    difficulty VARCHAR(20) NOT NULL DEFAULT 'BEGINNER',
    learner_count INT UNSIGNED NOT NULL DEFAULT 0,
    rating_score DECIMAL(3, 2) NOT NULL DEFAULT 0.00,
    post_count INT UNSIGNED NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_skill_topics_code (skill_code),
    KEY idx_skill_topics_status (status, learner_count DESC)
) ENGINE=InnoDB COMMENT='Skill topics for skill library';

-- Skill i18n table
CREATE TABLE IF NOT EXISTS skill_topic_i18n (
    id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    skill_id BIGINT UNSIGNED NOT NULL,
    language_code VARCHAR(16) NOT NULL,
    name VARCHAR(120) NOT NULL,
    description TEXT NULL,
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_skill_i18n_skill_lang (skill_id, language_code),
    KEY idx_skill_i18n_lang_name (language_code, name),
    CONSTRAINT fk_skill_i18n_skill_id FOREIGN KEY (skill_id) REFERENCES skill_topics (skill_id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='Localized skill topic names';

-- Learning roadmaps table
CREATE TABLE IF NOT EXISTS learning_roadmaps (
    roadmap_id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    skill_id BIGINT UNSIGNED NOT NULL,
    author_id BIGINT UNSIGNED NOT NULL,
    title VARCHAR(200) NOT NULL,
    summary TEXT NULL,
    difficulty VARCHAR(20) NOT NULL DEFAULT 'BEGINNER',
    estimated_days INT UNSIGNED NOT NULL DEFAULT 7,
    cover_image_url VARCHAR(512) NULL,
    rating_score DECIMAL(3, 2) NOT NULL DEFAULT 0.00,
    learner_count INT UNSIGNED NOT NULL DEFAULT 0,
    node_count INT UNSIGNED NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED',
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_roadmaps_skill (skill_id, status),
    KEY idx_roadmaps_author (author_id, status),
    KEY idx_roadmaps_learners (learner_count DESC),
    CONSTRAINT fk_roadmaps_skill_id FOREIGN KEY (skill_id) REFERENCES skill_topics (skill_id) ON DELETE CASCADE,
    CONSTRAINT fk_roadmaps_author_id FOREIGN KEY (author_id) REFERENCES users (user_id)
) ENGINE=InnoDB COMMENT='Learning roadmaps for skills';

-- Roadmap nodes table
CREATE TABLE IF NOT EXISTS roadmap_nodes (
    node_id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    roadmap_id BIGINT UNSIGNED NOT NULL,
    sort_no INT UNSIGNED NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT NULL,
    estimated_minutes INT UNSIGNED NOT NULL DEFAULT 60,
    related_post_id BIGINT UNSIGNED NULL,
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_roadmap_nodes_roadmap_sort (roadmap_id, sort_no),
    CONSTRAINT fk_roadmap_nodes_roadmap_id FOREIGN KEY (roadmap_id) REFERENCES learning_roadmaps (roadmap_id) ON DELETE CASCADE,
    CONSTRAINT fk_roadmap_nodes_post_id FOREIGN KEY (related_post_id) REFERENCES posts (post_id) ON DELETE SET NULL
) ENGINE=InnoDB COMMENT='Nodes within a learning roadmap';

-- User roadmap progress table
CREATE TABLE IF NOT EXISTS user_roadmap_progress (
    progress_id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    roadmap_id BIGINT UNSIGNED NOT NULL,
    current_node_id BIGINT UNSIGNED NULL,
    completed_nodes INT UNSIGNED NOT NULL DEFAULT 0,
    progress_percent DECIMAL(5, 2) NOT NULL DEFAULT 0.00,
    status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    started_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_roadmap (user_id, roadmap_id),
    KEY idx_user_roadmap_status (user_id, status),
    CONSTRAINT fk_user_roadmap_user_id FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roadmap_roadmap_id FOREIGN KEY (roadmap_id) REFERENCES learning_roadmaps (roadmap_id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roadmap_node_id FOREIGN KEY (current_node_id) REFERENCES roadmap_nodes (node_id) ON DELETE SET NULL
) ENGINE=InnoDB COMMENT='User progress on learning roadmaps';

-- User completed nodes table
CREATE TABLE IF NOT EXISTS user_completed_nodes (
    id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    roadmap_id BIGINT UNSIGNED NOT NULL,
    node_id BIGINT UNSIGNED NOT NULL,
    completed_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_completed_node (user_id, node_id),
    KEY idx_user_completed_roadmap (user_id, roadmap_id),
    CONSTRAINT fk_user_completed_user_id FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE,
    CONSTRAINT fk_user_completed_roadmap_id FOREIGN KEY (roadmap_id) REFERENCES learning_roadmaps (roadmap_id) ON DELETE CASCADE,
    CONSTRAINT fk_user_completed_node_id FOREIGN KEY (node_id) REFERENCES roadmap_nodes (node_id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='Completed roadmap nodes by users';

-- User skill subscriptions
CREATE TABLE IF NOT EXISTS user_skill_subscriptions (
    id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    skill_id BIGINT UNSIGNED NOT NULL,
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_skill (user_id, skill_id),
    CONSTRAINT fk_user_skill_user_id FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE,
    CONSTRAINT fk_user_skill_skill_id FOREIGN KEY (skill_id) REFERENCES skill_topics (skill_id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='User subscriptions to skill topics';

-- Seed data for skill topics
INSERT INTO skill_topics (skill_code, difficulty, learner_count, rating_score, post_count) VALUES
('java_backend', 'INTERMEDIATE', 1250, 4.65, 89),
('vue_frontend', 'BEGINNER', 980, 4.50, 67),
('spring_mvc', 'INTERMEDIATE', 850, 4.45, 54),
('mybatis', 'BEGINNER', 720, 4.30, 42),
('mysql', 'INTERMEDIATE', 1100, 4.55, 76),
('algorithm', 'ADVANCED', 650, 4.70, 45),
('english_cet4', 'BEGINNER', 1500, 4.20, 38),
('math_graduate', 'ADVANCED', 420, 4.60, 31),
('aigc_tools', 'BEGINNER', 1800, 4.40, 95),
('ai_product', 'INTERMEDIATE', 560, 4.35, 28),
('paper_writing', 'INTERMEDIATE', 380, 4.25, 22),
('python_data', 'BEGINNER', 890, 4.48, 58),
('docker_devops', 'INTERMEDIATE', 450, 4.55, 35),
('react_native', 'INTERMEDIATE', 320, 4.40, 25)
ON DUPLICATE KEY UPDATE learner_count = VALUES(learner_count);

-- Seed data for skill i18n
INSERT INTO skill_topic_i18n (skill_id, language_code, name, description) VALUES
(1, 'zh_CN', 'Java 后端', '学习 Java 后端开发，包括 Spring Boot、MyBatis、数据库等核心技术'),
(1, 'en_US', 'Java Backend', 'Learn Java backend development with Spring Boot, MyBatis, and database technologies'),
(2, 'zh_CN', 'Vue 前端', '学习 Vue.js 前端框架，掌握组件化开发和现代前端工程化'),
(2, 'en_US', 'Vue Frontend', 'Learn Vue.js frontend framework, component-based development and modern frontend engineering'),
(3, 'zh_CN', 'Spring MVC', '深入学习 Spring MVC 框架，掌握 Web 应用开发核心技能'),
(3, 'en_US', 'Spring MVC', 'Deep dive into Spring MVC framework for web application development'),
(4, 'zh_CN', 'MyBatis', '学习 MyBatis ORM 框架，掌握数据库访问和映射技术'),
(4, 'en_US', 'MyBatis', 'Learn MyBatis ORM framework for database access and mapping'),
(5, 'zh_CN', 'MySQL 数据库', '学习 MySQL 数据库设计、优化和管理'),
(5, 'en_US', 'MySQL Database', 'Learn MySQL database design, optimization and administration'),
(6, 'zh_CN', '算法刷题', '系统学习数据结构与算法，提升编程能力'),
(6, 'en_US', 'Algorithm Practice', 'Systematic learning of data structures and algorithms'),
(7, 'zh_CN', '英语四级', '备考英语四级考试，提升英语综合能力'),
(7, 'en_US', 'CET-4 English', 'Prepare for CET-4 English exam'),
(8, 'zh_CN', '考研数学', '系统学习高等数学、线性代数和概率统计'),
(8, 'en_US', 'Graduate Math', 'Systematic learning of advanced math, linear algebra and probability'),
(9, 'zh_CN', 'AIGC 工具', '学习使用各种 AI 生成内容工具，提升创作效率'),
(9, 'en_US', 'AIGC Tools', 'Learn to use various AI content generation tools'),
(10, 'zh_CN', 'AI 产品设计', '学习 AI 产品设计理念和方法'),
(10, 'en_US', 'AI Product Design', 'Learn AI product design concepts and methods'),
(11, 'zh_CN', '论文写作', '学习学术论文写作方法和技巧'),
(11, 'en_US', 'Paper Writing', 'Learn academic paper writing methods and techniques'),
(12, 'zh_CN', 'Python 数据分析', '学习 Python 数据处理、可视化和机器学习基础'),
(12, 'en_US', 'Python Data Analysis', 'Learn Python data processing, visualization and ML basics'),
(13, 'zh_CN', 'Docker 与 DevOps', '学习容器化技术和 DevOps 实践'),
(13, 'en_US', 'Docker & DevOps', 'Learn containerization and DevOps practices'),
(14, 'zh_CN', 'React Native', '学习使用 React Native 开发跨平台移动应用'),
(14, 'en_US', 'React Native', 'Learn cross-platform mobile app development with React Native')
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- Seed data for learning roadmaps
INSERT INTO learning_roadmaps (skill_id, author_id, title, summary, difficulty, estimated_days, learner_count, node_count) VALUES
(1, 1, 'Spring MVC + MyBatis 课设速通路线', '适合大二 Java Web 课程设计，7 天完成从零到部署的完整项目', 'INTERMEDIATE', 7, 320, 7),
(1, 1, 'Java 后端入门路线', '零基础入门 Java 后端开发，系统学习核心技术栈', 'BEGINNER', 30, 580, 10),
(2, 1, 'Vue 3 实战入门', '从零开始学习 Vue 3，通过实际项目掌握核心概念', 'BEGINNER', 14, 420, 8),
(5, 1, 'MySQL 从入门到精通', '系统学习 MySQL 数据库，从基础查询到性能优化', 'INTERMEDIATE', 21, 350, 12),
(9, 1, 'AIGC 工具快速上手', '一周掌握主流 AI 工具，提升学习和工作效率', 'BEGINNER', 7, 680, 5),
(6, 1, '算法刷题入门路线', '适合准备面试或竞赛的同学，系统刷题提升算法能力', 'INTERMEDIATE', 30, 280, 15)
ON DUPLICATE KEY UPDATE title = VALUES(title);

-- Seed data for roadmap nodes
INSERT INTO roadmap_nodes (roadmap_id, sort_no, title, description, estimated_minutes, related_post_id) VALUES
(1, 1, '搭建项目结构', '创建 Maven 项目，配置 Spring MVC 和 MyBatis', 120, NULL),
(1, 2, '配置 MyBatis', '配置数据源、映射文件和实体类', 90, NULL),
(1, 3, '完成用户登录', '实现用户登录、注册和权限验证', 180, NULL),
(1, 4, '完成内容发布', '实现文章发布、编辑和展示功能', 180, NULL),
(1, 5, '完成评论互动', '实现评论、点赞和收藏功能', 150, NULL),
(1, 6, '接入 AI 摘要', '调用 AI API 生成文章摘要', 120, NULL),
(1, 7, '部署与答辩包装', '项目部署和答辩材料准备', 120, NULL),
(2, 1, 'Java 基础语法', '学习 Java 基本语法、数据类型和控制结构', 240, NULL),
(2, 2, '面向对象编程', '掌握类、对象、继承、多态等核心概念', 300, NULL),
(2, 3, '集合框架', '学习 List、Map、Set 等常用集合', 180, NULL),
(3, 1, 'Vue 3 基础', '学习 Vue 3 核心概念：响应式、组件、模板', 180, NULL),
(3, 2, '组合式 API', '掌握 setup、ref、computed、watch 等', 150, NULL),
(3, 3, '路由和状态管理', '学习 Vue Router 和 Pinia', 180, NULL),
(5, 1, 'ChatGPT 提示词工程', '学习如何编写有效的 AI 提示词', 120, NULL),
(5, 2, 'AI 写作工具', '掌握 AI 辅助写作和内容生成', 90, NULL),
(5, 3, 'AI 图像工具', '学习使用 Midjourney、DALL-E 等工具', 90, NULL),
(5, 4, 'AI 编程助手', '掌握 GitHub Copilot 等 AI 编程工具', 90, NULL),
(5, 5, 'AI 工作流整合', '将 AI 工具整合到日常工作流程', 60, NULL)
ON DUPLICATE KEY UPDATE title = VALUES(title);
