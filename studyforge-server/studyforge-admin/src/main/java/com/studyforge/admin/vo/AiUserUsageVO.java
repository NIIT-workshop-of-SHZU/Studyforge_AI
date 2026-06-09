package com.studyforge.admin.vo;

import java.math.BigDecimal;

/**
 * 用户 AI 使用统计 VO
 */
public class AiUserUsageVO {
    private Long userId;
    private String username;
    private String displayName;
    
    // 总体统计
    private Integer totalCalls;
    private Integer totalPromptTokens;
    private Integer totalCompletionTokens;
    private Integer totalTokens;
    private BigDecimal totalCostYuan;
    
    // 按功能类型统计
    private Integer summaryCalls;
    private Integer reviewCardCalls;
    private Integer questionCalls;
    private Integer markdownFormatCalls;
    
    // 成功率
    private Integer successfulCalls;
    private Integer failedCalls;
    private Double successRate;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Integer getTotalCalls() {
        return totalCalls;
    }

    public void setTotalCalls(Integer totalCalls) {
        this.totalCalls = totalCalls;
    }

    public Integer getTotalPromptTokens() {
        return totalPromptTokens;
    }

    public void setTotalPromptTokens(Integer totalPromptTokens) {
        this.totalPromptTokens = totalPromptTokens;
    }

    public Integer getTotalCompletionTokens() {
        return totalCompletionTokens;
    }

    public void setTotalCompletionTokens(Integer totalCompletionTokens) {
        this.totalCompletionTokens = totalCompletionTokens;
    }

    public Integer getTotalTokens() {
        return totalTokens;
    }

    public void setTotalTokens(Integer totalTokens) {
        this.totalTokens = totalTokens;
    }

    public BigDecimal getTotalCostYuan() {
        return totalCostYuan;
    }

    public void setTotalCostYuan(BigDecimal totalCostYuan) {
        this.totalCostYuan = totalCostYuan;
    }

    public Integer getSummaryCalls() {
        return summaryCalls;
    }

    public void setSummaryCalls(Integer summaryCalls) {
        this.summaryCalls = summaryCalls;
    }

    public Integer getReviewCardCalls() {
        return reviewCardCalls;
    }

    public void setReviewCardCalls(Integer reviewCardCalls) {
        this.reviewCardCalls = reviewCardCalls;
    }

    public Integer getQuestionCalls() {
        return questionCalls;
    }

    public void setQuestionCalls(Integer questionCalls) {
        this.questionCalls = questionCalls;
    }

    public Integer getMarkdownFormatCalls() {
        return markdownFormatCalls;
    }

    public void setMarkdownFormatCalls(Integer markdownFormatCalls) {
        this.markdownFormatCalls = markdownFormatCalls;
    }

    public Integer getSuccessfulCalls() {
        return successfulCalls;
    }

    public void setSuccessfulCalls(Integer successfulCalls) {
        this.successfulCalls = successfulCalls;
    }

    public Integer getFailedCalls() {
        return failedCalls;
    }

    public void setFailedCalls(Integer failedCalls) {
        this.failedCalls = failedCalls;
    }

    public Double getSuccessRate() {
        return successRate;
    }

    public void setSuccessRate(Double successRate) {
        this.successRate = successRate;
    }
}
