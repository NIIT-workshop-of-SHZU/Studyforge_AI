package com.studyforge.admin.vo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * AI使用概览VO - 仅包含脱敏后的统计数据
 * 安全设计原则：
 * 1. 不包含任何用户个人信息
 * 2. 不包含API密钥等敏感配置
 * 3. 只返回聚合统计数据
 * 4. 所有数值类型明确，避免溢出
 */
public class AiUsageOverviewVO implements Serializable {
    private static final long serialVersionUID = 1L;

    // 今日统计
    private Integer todayTotalCalls;        // 今日总调用次数
    private Integer todayUniqueUsers;       // 今日独立用户数（已脱敏，只显示数量）
    private Integer todaySuccessfulCalls;   // 今日成功调用数
    private Integer todayFailedCalls;       // 今日失败调用数
    private BigDecimal todaySuccessRate;    // 今日成功率（百分比）
    
    // 本月统计
    private Integer monthTotalCalls;        // 本月总调用次数
    private BigDecimal estimatedCost;       // 预估费用（不显示详细计算方式）
    
    // 功能分布（只返回类型和数量，不暴露具体内容）
    private List<FeatureDistributionItem> featureDistribution;
    
    // 缓存统计
    private Integer cacheHitCount;          // 缓存命中次数
    private BigDecimal cacheHitRate;        // 缓存命中率
    
    // Getter and Setter
    public Integer getTodayTotalCalls() {
        return todayTotalCalls;
    }
    
    public void setTodayTotalCalls(Integer todayTotalCalls) {
        this.todayTotalCalls = todayTotalCalls;
    }
    
    public Integer getTodayUniqueUsers() {
        return todayUniqueUsers;
    }
    
    public void setTodayUniqueUsers(Integer todayUniqueUsers) {
        this.todayUniqueUsers = todayUniqueUsers;
    }
    
    public Integer getTodaySuccessfulCalls() {
        return todaySuccessfulCalls;
    }
    
    public void setTodaySuccessfulCalls(Integer todaySuccessfulCalls) {
        this.todaySuccessfulCalls = todaySuccessfulCalls;
    }
    
    public Integer getTodayFailedCalls() {
        return todayFailedCalls;
    }
    
    public void setTodayFailedCalls(Integer todayFailedCalls) {
        this.todayFailedCalls = todayFailedCalls;
    }
    
    public BigDecimal getTodaySuccessRate() {
        return todaySuccessRate;
    }
    
    public void setTodaySuccessRate(BigDecimal todaySuccessRate) {
        this.todaySuccessRate = todaySuccessRate;
    }
    
    public Integer getMonthTotalCalls() {
        return monthTotalCalls;
    }
    
    public void setMonthTotalCalls(Integer monthTotalCalls) {
        this.monthTotalCalls = monthTotalCalls;
    }
    
    public BigDecimal getEstimatedCost() {
        return estimatedCost;
    }
    
    public void setEstimatedCost(BigDecimal estimatedCost) {
        this.estimatedCost = estimatedCost;
    }
    
    public List<FeatureDistributionItem> getFeatureDistribution() {
        return featureDistribution;
    }
    
    public void setFeatureDistribution(List<FeatureDistributionItem> featureDistribution) {
        this.featureDistribution = featureDistribution;
    }
    
    public Integer getCacheHitCount() {
        return cacheHitCount;
    }
    
    public void setCacheHitCount(Integer cacheHitCount) {
        this.cacheHitCount = cacheHitCount;
    }
    
    public BigDecimal getCacheHitRate() {
        return cacheHitRate;
    }
    
    public void setCacheHitRate(BigDecimal cacheHitRate) {
        this.cacheHitRate = cacheHitRate;
    }
    
    /**
     * 功能分布项 - 只包含类型和数量
     */
    public static class FeatureDistributionItem implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private String featureType;     // 功能类型：SUMMARY/REVIEW_CARD/QA/FORMAT
        private Integer callCount;      // 调用次数
        private BigDecimal percentage;  // 占比百分比
        
        public String getFeatureType() {
            return featureType;
        }
        
        public void setFeatureType(String featureType) {
            this.featureType = featureType;
        }
        
        public Integer getCallCount() {
            return callCount;
        }
        
        public void setCallCount(Integer callCount) {
            this.callCount = callCount;
        }
        
        public BigDecimal getPercentage() {
            return percentage;
        }
        
        public void setPercentage(BigDecimal percentage) {
            this.percentage = percentage;
        }
    }
}
