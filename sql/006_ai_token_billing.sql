-- =============================================
-- AI Token 计费和价格配置
-- 创建时间：2026-06-02
-- =============================================

-- 1. 为 ai_logs 表添加 token 统计字段
-- 注意：这些字段允许 NULL，以便向后兼容旧数据

-- 添加 prompt_tokens 字段
SET @alter_sql = (SELECT IF(COUNT(*) = 0, 
    'ALTER TABLE `ai_logs` ADD COLUMN `prompt_tokens` INT UNSIGNED NULL COMMENT "输入token数" AFTER `response_text`', 
    'DO 0') 
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_logs' AND COLUMN_NAME = 'prompt_tokens');
PREPARE stmt FROM @alter_sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 添加 completion_tokens 字段
SET @alter_sql = (SELECT IF(COUNT(*) = 0, 
    'ALTER TABLE `ai_logs` ADD COLUMN `completion_tokens` INT UNSIGNED NULL COMMENT "输出token数" AFTER `prompt_tokens`', 
    'DO 0') 
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_logs' AND COLUMN_NAME = 'completion_tokens');
PREPARE stmt FROM @alter_sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 添加 total_tokens 字段
SET @alter_sql = (SELECT IF(COUNT(*) = 0, 
    'ALTER TABLE `ai_logs` ADD COLUMN `total_tokens` INT UNSIGNED NULL COMMENT "总token数" AFTER `completion_tokens`', 
    'DO 0') 
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_logs' AND COLUMN_NAME = 'total_tokens');
PREPARE stmt FROM @alter_sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 添加 cost_yuan 字段（费用，元）
SET @alter_sql = (SELECT IF(COUNT(*) = 0, 
    'ALTER TABLE `ai_logs` ADD COLUMN `cost_yuan` DECIMAL(10, 6) NULL COMMENT "本次调用费用（元）" AFTER `total_tokens`', 
    'DO 0') 
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_logs' AND COLUMN_NAME = 'cost_yuan');
PREPARE stmt FROM @alter_sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 添加 model_name 字段
SET @alter_sql = (SELECT IF(COUNT(*) = 0, 
    'ALTER TABLE `ai_logs` ADD COLUMN `model_name` VARCHAR(100) NULL COMMENT "使用的模型名称" AFTER `ai_type`', 
    'DO 0') 
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_logs' AND COLUMN_NAME = 'model_name');
PREPARE stmt FROM @alter_sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2. 创建 AI 模型价格配置表
CREATE TABLE IF NOT EXISTS ai_model_pricing (
    pricing_id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    model_name VARCHAR(100) NOT NULL COMMENT '模型名称',
    prompt_price_per_1k DECIMAL(10, 6) NOT NULL COMMENT '输入token价格（元/千tokens）',
    completion_price_per_1k DECIMAL(10, 6) NOT NULL COMMENT '输出token价格（元/千tokens）',
    currency VARCHAR(10) NOT NULL DEFAULT 'CNY' COMMENT '货币单位',
    effective_date DATE NOT NULL COMMENT '价格生效日期',
    is_active TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
    remark VARCHAR(500) NULL COMMENT '备注',
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_ai_model_pricing_model_date (model_name, effective_date),
    KEY idx_ai_model_pricing_active (is_active, effective_date DESC)
) ENGINE=InnoDB COMMENT='AI模型价格配置表';

-- 3. 插入硅基流动常用模型的价格配置
-- 价格参考：https://siliconflow.cn/zh-cn/pricing
INSERT INTO ai_model_pricing (model_name, prompt_price_per_1k, completion_price_per_1k, effective_date, is_active, remark) VALUES
('deepseek-ai/DeepSeek-V4-Flash', 0.002, 0.008, CURDATE(), 1, '硅基流动 - 推荐模型'),
('deepseek-ai/DeepSeek-V3', 0.002, 0.008, CURDATE(), 1, '硅基流动'),
('deepseek-ai/DeepSeek-R1', 0.004, 0.016, CURDATE(), 1, '硅基流动 - 推理模型'),
('Qwen/Qwen2.5-72B-Instruct', 0.004, 0.016, CURDATE(), 1, '硅基流动 - 通义千问'),
('THUDM/glm-4-9b-chat', 0.001, 0.001, CURDATE(), 1, '硅基流动 - 智谱GLM'),
('meta-llama/Llama-3.3-70B-Instruct', 0.004, 0.016, CURDATE(), 1, '硅基流动 - Llama'),
('google/gemma-2-9b-it', 0.001, 0.001, CURDATE(), 1, '硅基流动 - Gemma')
ON DUPLICATE KEY UPDATE
    prompt_price_per_1k = VALUES(prompt_price_per_1k),
    completion_price_per_1k = VALUES(completion_price_per_1k),
    is_active = VALUES(is_active);

-- 4. 验证表结构
SELECT '===== AI Logs 表结构 =====' AS info;
DESCRIBE ai_logs;

SELECT '===== AI 模型价格配置 =====' AS info;
SELECT 
    model_name,
    CONCAT('¥', prompt_price_per_1k, '/1K') AS input_price,
    CONCAT('¥', completion_price_per_1k, '/1K') AS output_price,
    effective_date,
    CASE WHEN is_active = 1 THEN '✓ 启用' ELSE '✗ 停用' END AS status
FROM ai_model_pricing
WHERE is_active = 1
ORDER BY model_name;

SELECT '===== 配置完成！===== ' AS info;
