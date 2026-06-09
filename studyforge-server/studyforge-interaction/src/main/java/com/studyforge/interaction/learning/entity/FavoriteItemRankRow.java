package com.studyforge.interaction.learning.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class FavoriteItemRankRow {
    private Long id;
    private Long collectionId;
    private Long postId;
    private Long userId;
    private LocalDateTime createdTime;
    private BigDecimal importanceScore;
    private Integer pinned;
    private String scoreFactors;
    private Long categoryId;
    private String aiTags;
    private String title;
    private String summary;
    private String originalLanguage;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCollectionId() {
        return collectionId;
    }

    public void setCollectionId(Long collectionId) {
        this.collectionId = collectionId;
    }

    public Long getPostId() {
        return postId;
    }

    public void setPostId(Long postId) {
        this.postId = postId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public LocalDateTime getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(LocalDateTime createdTime) {
        this.createdTime = createdTime;
    }

    public BigDecimal getImportanceScore() {
        return importanceScore;
    }

    public void setImportanceScore(BigDecimal importanceScore) {
        this.importanceScore = importanceScore;
    }

    public Integer getPinned() {
        return pinned;
    }

    public void setPinned(Integer pinned) {
        this.pinned = pinned;
    }

    public String getScoreFactors() {
        return scoreFactors;
    }

    public void setScoreFactors(String scoreFactors) {
        this.scoreFactors = scoreFactors;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getAiTags() {
        return aiTags;
    }

    public void setAiTags(String aiTags) {
        this.aiTags = aiTags;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getOriginalLanguage() {
        return originalLanguage;
    }

    public void setOriginalLanguage(String originalLanguage) {
        this.originalLanguage = originalLanguage;
    }
}
