package com.studyforge.admin.service.impl;

import com.studyforge.admin.service.AiUserUsageService;
import com.studyforge.admin.vo.AiUserUsageVO;
import com.studyforge.common.exception.BizException;
import com.studyforge.common.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 用户 AI 使用统计服务实现
 */
@Service
public class AiUserUsageServiceImpl implements AiUserUsageService {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Override
    public AiUserUsageVO getUserUsage(Long userId) {
        String sql = """
            SELECT 
                u.user_id,
                u.username,
                u.display_name,
                COUNT(l.log_id) AS total_calls,
                COALESCE(SUM(l.prompt_tokens), 0) AS total_prompt_tokens,
                COALESCE(SUM(l.completion_tokens), 0) AS total_completion_tokens,
                COALESCE(SUM(l.total_tokens), 0) AS total_tokens,
                COALESCE(SUM(l.cost_yuan), 0) AS total_cost_yuan,
                COUNT(CASE WHEN l.ai_type = 'SUMMARY' THEN 1 END) AS summary_calls,
                COUNT(CASE WHEN l.ai_type = 'REVIEW_CARD' THEN 1 END) AS review_card_calls,
                COUNT(CASE WHEN l.ai_type = 'QUESTION' THEN 1 END) AS question_calls,
                COUNT(CASE WHEN l.ai_type = 'MARKDOWN_FORMAT' THEN 1 END) AS markdown_format_calls,
                COUNT(CASE WHEN l.success = 1 THEN 1 END) AS successful_calls,
                COUNT(CASE WHEN l.success = 0 THEN 1 END) AS failed_calls
            FROM users u
            LEFT JOIN ai_logs l ON u.user_id = l.user_id
            WHERE u.user_id = ?
            GROUP BY u.user_id, u.username, u.display_name
            """;
        
        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, userId);
        
        if (results.isEmpty()) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        
        return mapToVO(results.get(0));
    }
    
    @Override
    public List<AiUserUsageVO> getUserUsageRanking(int limit) {
        // 限制最大查询数量
        if (limit <= 0 || limit > 100) {
            limit = 50;
        }
        
        String sql = """
            SELECT 
                u.user_id,
                u.username,
                u.display_name,
                COUNT(l.log_id) AS total_calls,
                COALESCE(SUM(l.prompt_tokens), 0) AS total_prompt_tokens,
                COALESCE(SUM(l.completion_tokens), 0) AS total_completion_tokens,
                COALESCE(SUM(l.total_tokens), 0) AS total_tokens,
                COALESCE(SUM(l.cost_yuan), 0) AS total_cost_yuan,
                COUNT(CASE WHEN l.ai_type = 'SUMMARY' THEN 1 END) AS summary_calls,
                COUNT(CASE WHEN l.ai_type = 'REVIEW_CARD' THEN 1 END) AS review_card_calls,
                COUNT(CASE WHEN l.ai_type = 'QUESTION' THEN 1 END) AS question_calls,
                COUNT(CASE WHEN l.ai_type = 'MARKDOWN_FORMAT' THEN 1 END) AS markdown_format_calls,
                COUNT(CASE WHEN l.success = 1 THEN 1 END) AS successful_calls,
                COUNT(CASE WHEN l.success = 0 THEN 1 END) AS failed_calls
            FROM users u
            LEFT JOIN ai_logs l ON u.user_id = l.user_id
            GROUP BY u.user_id, u.username, u.display_name
            HAVING COUNT(l.log_id) > 0
            ORDER BY total_cost_yuan DESC
            LIMIT ?
            """;
        
        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, limit);
        
        return results.stream()
                .map(this::mapToVO)
                .toList();
    }
    
    /**
     * 将查询结果映射为 VO
     */
    private AiUserUsageVO mapToVO(Map<String, Object> data) {
        AiUserUsageVO vo = new AiUserUsageVO();
        
        vo.setUserId(getLongValue(data.get("user_id")));
        vo.setUsername(getStringValue(data.get("username")));
        vo.setDisplayName(getStringValue(data.get("display_name")));
        
        vo.setTotalCalls(getIntValue(data.get("total_calls")));
        vo.setTotalPromptTokens(getIntValue(data.get("total_prompt_tokens")));
        vo.setTotalCompletionTokens(getIntValue(data.get("total_completion_tokens")));
        vo.setTotalTokens(getIntValue(data.get("total_tokens")));
        vo.setTotalCostYuan(getBigDecimalValue(data.get("total_cost_yuan")));
        
        vo.setSummaryCalls(getIntValue(data.get("summary_calls")));
        vo.setReviewCardCalls(getIntValue(data.get("review_card_calls")));
        vo.setQuestionCalls(getIntValue(data.get("question_calls")));
        vo.setMarkdownFormatCalls(getIntValue(data.get("markdown_format_calls")));
        
        vo.setSuccessfulCalls(getIntValue(data.get("successful_calls")));
        vo.setFailedCalls(getIntValue(data.get("failed_calls")));
        
        // 计算成功率
        int totalCalls = vo.getTotalCalls();
        if (totalCalls > 0) {
            double successRate = (vo.getSuccessfulCalls() * 100.0) / totalCalls;
            vo.setSuccessRate(Math.round(successRate * 100.0) / 100.0);
        } else {
            vo.setSuccessRate(0.0);
        }
        
        return vo;
    }
    
    private Long getLongValue(Object value) {
        if (value == null) return 0L;
        if (value instanceof Number) return ((Number) value).longValue();
        return Long.parseLong(value.toString());
    }
    
    private Integer getIntValue(Object value) {
        if (value == null) return 0;
        if (value instanceof Number) return ((Number) value).intValue();
        return Integer.parseInt(value.toString());
    }
    
    private BigDecimal getBigDecimalValue(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal) return (BigDecimal) value;
        if (value instanceof Number) return new BigDecimal(value.toString());
        return BigDecimal.ZERO;
    }
    
    private String getStringValue(Object value) {
        return value == null ? "" : value.toString();
    }
}
