package com.studyforge.system.vo;

import java.time.LocalDateTime;

public record CommunityHomepageDesignVO(Long designId,
                                        Long authorId,
                                        String authorName,
                                        String authorAvatarUrl,
                                        String title,
                                        String summary,
                                        String templateType,
                                        String layoutMode,
                                        String themeConfig,
                                        String customCode,
                                        String mediaLayout,
                                        String status,
                                        LocalDateTime submittedAt,
                                        LocalDateTime reviewDeadlineAt,
                                        String reviewComment,
                                        int cloneCount,
                                        int likeCount,
                                        int favoriteCount,
                                        int commentCount,
                                        LocalDateTime createdTime,
                                        boolean canClone) {
}
