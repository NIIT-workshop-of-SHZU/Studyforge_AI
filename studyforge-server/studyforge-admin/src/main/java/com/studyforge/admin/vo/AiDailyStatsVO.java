package com.studyforge.admin.vo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * AI调用趋势VO - 按天统计，不包含任何个人数据
 */
public class AiDailyStatsVO implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private LocalDate statDate;           // 统计日期
    private Integer totalCalls;           // 总调用次数
    private Integer uniqueUsers;          // 独立用户数（脱敏）
    private Integer successfulCalls;      // 成功调用数
    private BigDecimal successRate;       // 成功率
    
    public LocalDate getStatDate() {
        return statDate;
    }
    
    public void setStatDate(LocalDate statDate) {
        this.statDate = statDate;
    }
    
    public Integer getTotalCalls() {
        return totalCalls;
    }
    
    public void setTotalCalls(Integer totalCalls) {
        this.totalCalls = totalCalls;
    }
    
    public Integer getUniqueUsers() {
        return uniqueUsers;
    }
    
    public void setUniqueUsers(Integer uniqueUsers) {
        this.uniqueUsers = uniqueUsers;
    }
    
    public Integer getSuccessfulCalls() {
        return successfulCalls;
    }
    
    public void setSuccessfulCalls(Integer successfulCalls) {
        this.successfulCalls = successfulCalls;
    }
    
    public BigDecimal getSuccessRate() {
        return successRate;
    }
    
    public void setSuccessRate(BigDecimal successRate) {
        this.successRate = successRate;
    }
}
