package com.studyforge.interaction.learning.entity;

import java.time.LocalDateTime;

public class UserLearningProfile {
    private Long userId;
    private String memoryMd;
    private Integer memoryManuallyEdited;
    private String interestTags;
    private String embeddingJson;
    private Integer profileVersion;
    private Integer aiMemoryEnabled;
    private LocalDateTime lastRefreshedAt;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getMemoryMd() {
        return memoryMd;
    }

    public void setMemoryMd(String memoryMd) {
        this.memoryMd = memoryMd;
    }

    public Integer getMemoryManuallyEdited() {
        return memoryManuallyEdited;
    }

    public void setMemoryManuallyEdited(Integer memoryManuallyEdited) {
        this.memoryManuallyEdited = memoryManuallyEdited;
    }

    public String getInterestTags() {
        return interestTags;
    }

    public void setInterestTags(String interestTags) {
        this.interestTags = interestTags;
    }

    public String getEmbeddingJson() {
        return embeddingJson;
    }

    public void setEmbeddingJson(String embeddingJson) {
        this.embeddingJson = embeddingJson;
    }

    public Integer getProfileVersion() {
        return profileVersion;
    }

    public void setProfileVersion(Integer profileVersion) {
        this.profileVersion = profileVersion;
    }

    public Integer getAiMemoryEnabled() {
        return aiMemoryEnabled;
    }

    public void setAiMemoryEnabled(Integer aiMemoryEnabled) {
        this.aiMemoryEnabled = aiMemoryEnabled;
    }

    public LocalDateTime getLastRefreshedAt() {
        return lastRefreshedAt;
    }

    public void setLastRefreshedAt(LocalDateTime lastRefreshedAt) {
        this.lastRefreshedAt = lastRefreshedAt;
    }

    public LocalDateTime getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(LocalDateTime createdTime) {
        this.createdTime = createdTime;
    }

    public LocalDateTime getUpdatedTime() {
        return updatedTime;
    }

    public void setUpdatedTime(LocalDateTime updatedTime) {
        this.updatedTime = updatedTime;
    }
}
