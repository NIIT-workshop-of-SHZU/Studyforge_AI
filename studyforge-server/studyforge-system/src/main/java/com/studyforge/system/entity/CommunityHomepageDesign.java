package com.studyforge.system.entity;

import java.time.LocalDateTime;

public class CommunityHomepageDesign {
    private Long designId;
    private Long authorId;
    private Long sourceHomepageId;
    private String title;
    private String summary;
    private String templateType;
    private String layoutMode;
    private String themeConfig;
    private String customCode;
    private String mediaLayout;
    private String status;
    private LocalDateTime submittedAt;
    private LocalDateTime reviewDeadlineAt;
    private Long reviewedBy;
    private LocalDateTime reviewedAt;
    private String reviewComment;
    private Integer cloneCount;
    private Integer likeCount;
    private Integer favoriteCount;
    private Integer commentCount;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
    private String authorName;
    private String authorAvatarUrl;

    public Long getDesignId() {
        return designId;
    }

    public void setDesignId(Long designId) {
        this.designId = designId;
    }

    public Long getAuthorId() {
        return authorId;
    }

    public void setAuthorId(Long authorId) {
        this.authorId = authorId;
    }

    public Long getSourceHomepageId() {
        return sourceHomepageId;
    }

    public void setSourceHomepageId(Long sourceHomepageId) {
        this.sourceHomepageId = sourceHomepageId;
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

    public String getCustomCode() {
        return customCode;
    }

    public void setCustomCode(String customCode) {
        this.customCode = customCode;
    }

    public String getMediaLayout() {
        return mediaLayout;
    }

    public void setMediaLayout(String mediaLayout) {
        this.mediaLayout = mediaLayout;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public Integer getCloneCount() {
        return cloneCount;
    }

    public void setCloneCount(Integer cloneCount) {
        this.cloneCount = cloneCount;
    }

    public Integer getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(Integer likeCount) {
        this.likeCount = likeCount;
    }

    public Integer getFavoriteCount() {
        return favoriteCount;
    }

    public void setFavoriteCount(Integer favoriteCount) {
        this.favoriteCount = favoriteCount;
    }

    public Integer getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(Integer commentCount) {
        this.commentCount = commentCount;
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

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getAuthorAvatarUrl() {
        return authorAvatarUrl;
    }

    public void setAuthorAvatarUrl(String authorAvatarUrl) {
        this.authorAvatarUrl = authorAvatarUrl;
    }
}
