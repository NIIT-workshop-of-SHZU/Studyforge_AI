package com.studyforge.admin.service.impl;

import com.studyforge.admin.service.AiUsageStatsService;
import com.studyforge.admin.vo.AiDailyStatsVO;
import com.studyforge.admin.vo.AiUsageOverviewVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * AI使用统计服务实现
 * 安全设计：
 * 1. 使用聚合SQL，不查询原始日志数据
 * 2. 限制查询时间范围，防止大数据量
 * 3. 所有数值进行边界检查
 */
@Service
public class AiUsageStatsServiceImpl implements AiUsageStatsService {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Override
    public AiUsageOverviewVO getUsageOverview() {
        AiUsageOverviewVO overview = new AiUsageOverviewVO();
        
        // 今日统计 - 使用聚合查询
        String todaySql = "SELECT " +
                "COUNT(*) as total_calls, " +
                "COUNT(DISTINCT user_id) as unique_users, " +
                "SUM(CASE WHEN success = 1 THEN 1 ELSE 0 END) as successful_calls, " +
                "SUM(CASE WHEN success = 0 THEN 1 ELSE 0 END) as failed_calls " +
                "FROM ai_logs WHERE DATE(created_time) = CURDATE()";
        
        Map<String, Object> todayResult = jdbcTemplate.queryForMap(todaySql);
        overview.setTodayTotalCalls(getIntValue(todayResult.get("total_calls")));
        overview.setTodayUniqueUsers(getIntValue(todayResult.get("unique_users")));
        overview.setTodaySuccessfulCalls(getIntValue(todayResult.get("successful_calls")));
        overview.setTodayFailedCalls(getIntValue(todayResult.get("failed_calls")));
        
        // 计算成功率
        int totalCalls = overview.getTodayTotalCalls();
        if (totalCalls > 0) {
            BigDecimal successRate = BigDecimal.valueOf(overview.getTodaySuccessfulCalls())
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(totalCalls), 2, RoundingMode.HALF_UP);
            overview.setTodaySuccessRate(successRate);
        } else {
            overview.setTodaySuccessRate(BigDecimal.ZERO);
        }
        
        // 本月统计
        String monthSql = "SELECT COUNT(*) as total_calls FROM ai_logs " +
                "WHERE YEAR(created_time) = YEAR(CURDATE()) AND MONTH(created_time) = MONTH(CURDATE())";
        Integer monthCalls = jdbcTemplate.queryForObject(monthSql, Integer.class);
        overview.setMonthTotalCalls(monthCalls != null ? monthCalls : 0);
        
        // 本月实际费用（聚合真实 cost_yuan，旧数据为 NULL 时计为 0）
        String monthCostSql = "SELECT COALESCE(SUM(cost_yuan), 0) FROM ai_logs " +
                "WHERE YEAR(created_time) = YEAR(CURDATE()) AND MONTH(created_time) = MONTH(CURDATE())";
        BigDecimal monthCost = jdbcTemplate.queryForObject(monthCostSql, BigDecimal.class);
        overview.setEstimatedCost(monthCost != null ? monthCost.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
        
        // 功能分布
        overview.setFeatureDistribution(getFeatureDistribution());
        
        // TODO: 缓存统计（需要实现缓存表后添加）
        overview.setCacheHitCount(0);
        overview.setCacheHitRate(BigDecimal.ZERO);
        
        return overview;
    }
    
    @Override
    public List<AiDailyStatsVO> getUsageTrend(int days) {
        // 安全限制：最多查询30天
        if (days > 30) {
            days = 30;
        }
        if (days < 1) {
            days = 7; // 默认7天
        }
        
        String sql = "SELECT " +
                "DATE(created_time) as stat_date, " +
                "COUNT(*) as total_calls, " +
                "COUNT(DISTINCT user_id) as unique_users, " +
                "SUM(CASE WHEN success = 1 THEN 1 ELSE 0 END) as successful_calls " +
                "FROM ai_logs " +
                "WHERE created_time >= DATE_SUB(CURDATE(), INTERVAL ? DAY) " +
                "GROUP BY DATE(created_time) " +
                "ORDER BY stat_date DESC";
        
        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, days);
        List<AiDailyStatsVO> statsList = new ArrayList<>();
        
        for (Map<String, Object> row : results) {
            AiDailyStatsVO stats = new AiDailyStatsVO();
            stats.setStatDate(((java.sql.Date) row.get("stat_date")).toLocalDate());
            stats.setTotalCalls(getIntValue(row.get("total_calls")));
            stats.setUniqueUsers(getIntValue(row.get("unique_users")));
            stats.setSuccessfulCalls(getIntValue(row.get("successful_calls")));
            
            // 计算成功率
            int total = stats.getTotalCalls();
            if (total > 0) {
                BigDecimal successRate = BigDecimal.valueOf(stats.getSuccessfulCalls())
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
                stats.setSuccessRate(successRate);
            } else {
                stats.setSuccessRate(BigDecimal.ZERO);
            }
            
            statsList.add(stats);
        }
        
        return statsList;
    }
    
    @Override
    public List<AiUsageOverviewVO.FeatureDistributionItem> getFeatureDistribution() {
        String sql = "SELECT " +
                "ai_type as feature_type, " +
                "COUNT(*) as call_count " +
                "FROM ai_logs " +
                "WHERE created_time >= DATE_SUB(CURDATE(), INTERVAL 30 DAY) " +
                "GROUP BY ai_type " +
                "ORDER BY call_count DESC";
        
        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);
        List<AiUsageOverviewVO.FeatureDistributionItem> distribution = new ArrayList<>();
        
        // 计算总数用于百分比
        int totalCount = results.stream()
                .mapToInt(row -> ((Number) row.get("call_count")).intValue())
                .sum();
        
        for (Map<String, Object> row : results) {
            AiUsageOverviewVO.FeatureDistributionItem item = 
                    new AiUsageOverviewVO.FeatureDistributionItem();
            
            item.setFeatureType((String) row.get("feature_type"));
            int count = getIntValue(row.get("call_count"));
            item.setCallCount(count);
            
            // 计算百分比
            if (totalCount > 0) {
                BigDecimal percentage = BigDecimal.valueOf(count)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(totalCount), 2, RoundingMode.HALF_UP);
                item.setPercentage(percentage);
            } else {
                item.setPercentage(BigDecimal.ZERO);
            }
            
            distribution.add(item);
        }
        
        return distribution;
    }
    
    /**
     * 安全地从Object获取int值，处理null情况
     */
    private int getIntValue(Object value) {
        if (value == null) {
            return 0;
        }
        return ((Number) value).intValue();
    }
}
