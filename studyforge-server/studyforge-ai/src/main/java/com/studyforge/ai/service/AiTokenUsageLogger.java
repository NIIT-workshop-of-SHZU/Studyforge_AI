package com.studyforge.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * AI Token 计费工具类
 * 
 * 功能：
 * 1. 从 API 响应中提取 token 使用情况
 * 2. 根据模型价格配置计算费用
 * 3. 更新 ai_logs 表的 token 统计字段
 * 
 * 设计原则：
 * - 独立工具类，不修改现有 AI Service 代码
 * - 在 Controller 层调用，记录每次 AI 调用的真实 token 消耗
 */
@Component
public class AiTokenUsageLogger {
    
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    
    private static final Logger log = LoggerFactory.getLogger(AiTokenUsageLogger.class);

    @Autowired
    public AiTokenUsageLogger(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = new ObjectMapper();  // 自己创建，不依赖 Spring 注入
    }
    
    /**
     * 从 API 响应 JSON 中提取 token 使用情况并记录到数据库
     * 
     * @param logId AI 日志 ID
     * @param apiResponse API 响应的 JSON 字符串
     * @param modelName 使用的模型名称
     */
    public void logTokenUsage(Long logId, String apiResponse, String modelName) {
        if (logId == null || apiResponse == null || apiResponse.isEmpty()) {
            return;
        }
        
        try {
            // 解析 API 响应
            JsonNode root = objectMapper.readTree(apiResponse);
            JsonNode usage = root.path("usage");
            
            if (usage.isMissingNode() || usage.isNull()) {
                // API 未返回 usage 信息，跳过
                return;
            }
            
            int promptTokens = usage.path("prompt_tokens").asInt(0);
            int completionTokens = usage.path("completion_tokens").asInt(0);
            int totalTokens = usage.path("total_tokens").asInt(0);
            
            // 如果 total_tokens 为 0，尝试计算
            if (totalTokens == 0 && (promptTokens > 0 || completionTokens > 0)) {
                totalTokens = promptTokens + completionTokens;
            }
            
            // 计算费用
            BigDecimal costYuan = calculateCost(modelName, promptTokens, completionTokens);
            
            // 更新 ai_logs 表
            updateAiLog(logId, modelName, promptTokens, completionTokens, totalTokens, costYuan);
            
        } catch (Exception e) {
            // 记录失败不影响主流程，只记录错误日志
            log.warn("[AI Token Logger] Failed to log token usage for logId={}", logId, e);
        }
    }
    
    /**
     * 根据模型价格配置计算费用
     * 
     * @param modelName 模型名称
     * @param promptTokens 输入 token 数
     * @param completionTokens 输出 token 数
     * @return 费用（元）
     */
    private BigDecimal calculateCost(String modelName, int promptTokens, int completionTokens) {
        if (modelName == null || modelName.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        try {
            // 查询价格配置
            String sql = "SELECT prompt_price_per_1k, completion_price_per_1k FROM ai_model_pricing " +
                        "WHERE model_name = ? AND is_active = 1 " +
                        "ORDER BY effective_date DESC LIMIT 1";
            
            var result = jdbcTemplate.queryForMap(sql, modelName);
            
            BigDecimal inputPrice = getBigDecimalValue(result.get("prompt_price_per_1k"));
            BigDecimal outputPrice = getBigDecimalValue(result.get("completion_price_per_1k"));
            
            // 计算费用：(prompt_tokens / 1000) * input_price + (completion_tokens / 1000) * output_price
            BigDecimal promptCost = BigDecimal.valueOf(promptTokens)
                    .divide(BigDecimal.valueOf(1000), 6, RoundingMode.HALF_UP)
                    .multiply(inputPrice);
            
            BigDecimal completionCost = BigDecimal.valueOf(completionTokens)
                    .divide(BigDecimal.valueOf(1000), 6, RoundingMode.HALF_UP)
                    .multiply(outputPrice);
            
            return promptCost.add(completionCost).setScale(6, RoundingMode.HALF_UP);
            
        } catch (Exception e) {
            // 如果查询价格失败，返回 0
            log.warn("[AI Token Logger] Failed to calculate cost for model: {}", modelName, e);
            return BigDecimal.ZERO;
        }
    }
    
    /**
     * 更新 ai_logs 表的 token 统计字段
     */
    private void updateAiLog(Long logId, String modelName, int promptTokens, 
                            int completionTokens, int totalTokens, BigDecimal costYuan) {
        String sql = "UPDATE ai_logs SET " +
                    "model_name = ?, " +
                    "prompt_tokens = ?, " +
                    "completion_tokens = ?, " +
                    "total_tokens = ?, " +
                    "cost_yuan = ? " +
                    "WHERE log_id = ?";
        
        jdbcTemplate.update(sql, modelName, promptTokens, completionTokens, totalTokens, costYuan, logId);
    }
    
    /**
     * 安全地从 Object 获取 BigDecimal 值
     */
    private BigDecimal getBigDecimalValue(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value instanceof Number) {
            return new BigDecimal(value.toString());
        }
        return BigDecimal.ZERO;
    }
}
