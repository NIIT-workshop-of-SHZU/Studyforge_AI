package com.studyforge.system.entity;

import java.time.LocalDateTime;

public class UserHomepage {
    private Long homepageId;
    private Long userId;
    private String templateType;
    private String layoutMode;
    private String themeConfig;
    private String customCodeDraft;
    private String mediaLayoutDraft;
    private String publishedTemplateType;
    private String publishedLayoutMode;
    private String publishedThemeConfig;
    private String publishedCustomCode;
    private String publishedMediaLayout;
    private Integer publishedVersion;
    private String reviewStatus;
    private LocalDateTime submittedAt;
    private LocalDateTime reviewDeadlineAt;
    private Long reviewedBy;
    private LocalDateTime reviewedAt;
    private String reviewComment;
    private Long communityTemplateId;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;

    public Long getHomepageId() {
        return homepageId;
    }

    public void setHomepageId(Long homepageId) {
        this.homepageId = homepageId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getTemplateType() {
        return templateType;
    }

    public void setTemplateType(String templateType) {
        this.templateType = templateType;
    }

    public String getLayoutMode() {
        return layoutMode;
    }

    public void setLayoutMode(String layoutMode) {
        this.layoutMode = layoutMode;
    }

    public String getThemeConfig() {
        return themeConfig;
    }

    public void setThemeConfig(String themeConfig) {
        this.themeConfig = themeConfig;
    }

    public String getCustomCodeDraft() {
        return customCodeDraft;
    }

    public void setCustomCodeDraft(String customCodeDraft) {
        this.customCodeDraft = customCodeDraft;
    }

    public String getMediaLayoutDraft() {
        return mediaLayoutDraft;
    }

    public void setMediaLayoutDraft(String mediaLayoutDraft) {
        this.mediaLayoutDraft = mediaLayoutDraft;
    }

    public String getPublishedTemplateType() {
        return publishedTemplateType;
    }

    public void setPublishedTemplateType(String publishedTemplateType) {
        this.publishedTemplateType = publishedTemplateType;
    }

    public String getPublishedLayoutMode() {
        return publishedLayoutMode;
    }

    public void setPublishedLayoutMode(String publishedLayoutMode) {
        this.publishedLayoutMode = publishedLayoutMode;
    }

    public String getPublishedThemeConfig() {
        return publishedThemeConfig;
    }

    public void setPublishedThemeConfig(String publishedThemeConfig) {
        this.publishedThemeConfig = publishedThemeConfig;
    }

    public String getPublishedCustomCode() {
        return publishedCustomCode;
    }

    public void setPublishedCustomCode(String publishedCustomCode) {
        this.publishedCustomCode = publishedCustomCode;
    }

    public String getPublishedMediaLayout() {
        return publishedMediaLayout;
    }

    public void setPublishedMediaLayout(String publishedMediaLayout) {
        this.publishedMediaLayout = publishedMediaLayout;
    }

    public Integer getPublishedVersion() {
        return publishedVersion;
    }

    public void setPublishedVersion(Integer publishedVersion) {
        this.publishedVersion = publishedVersion;
    }

    public String getReviewStatus() {
        return reviewStatus;
    }

    public void setReviewStatus(String reviewStatus) {
        this.reviewStatus = reviewStatus;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public LocalDateTime getReviewDeadlineAt() {
        return reviewDeadlineAt;
    }

    public void setReviewDeadlineAt(LocalDateTime reviewDeadlineAt) {
        this.reviewDeadlineAt = reviewDeadlineAt;
    }

    public Long getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(Long reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public String getReviewComment() {
        return reviewComment;
    }

    public void setReviewComment(String reviewComment) {
        this.reviewComment = reviewComment;
    }

    public Long getCommunityTemplateId() {
        return communityTemplateId;
    }

    public void setCommunityTemplateId(Long communityTemplateId) {
        this.communityTemplateId = communityTemplateId;
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
