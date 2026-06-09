package com.studyforge.content.vo;

import java.time.LocalDateTime;
import java.util.List;

public record PostSummaryVO(Long postId,
                            Long authorId,
                            String authorName,
                            String authorAvatarUrl,
                            String title,
                            String summary,
                            String languageCode,
                            String categoryCode,
                            String coverImageUrl,
                            int likeCount,
                            int favoriteCount,
                            int commentCount,
                            int viewCount,
                            double hotScore,
                            LocalDateTime createdTime,
                            LocalDateTime updatedTime,
                            LocalDateTime activityTime,
                            Double importanceScore,
                            Boolean pinned,
                            List<String> rankReasons) {
    public PostSummaryVO(Long postId,
                         Long authorId,
                         String authorName,
                         String authorAvatarUrl,
                         String title,
                         String summary,
                         String languageCode,
                         String categoryCode,
                         String coverImageUrl,
                         int likeCount,
                         int favoriteCount,
                         int commentCount,
                         int viewCount,
                         double hotScore,
                         LocalDateTime createdTime,
                         LocalDateTime updatedTime) {
        this(
                postId,
                authorId,
                authorName,
                authorAvatarUrl,
                title,
                summary,
                languageCode,
                categoryCode,
                coverImageUrl,
                likeCount,
                favoriteCount,
                commentCount,
                viewCount,
                hotScore,
                createdTime,
                updatedTime,
                null,
                null,
                null,
                null
        );
    }

    public PostSummaryVO withRanking(double score, boolean pinnedValue, List<String> reasons) {
        return new PostSummaryVO(
                postId,
                authorId,
                authorName,
                authorAvatarUrl,
                title,
                summary,
                languageCode,
                categoryCode,
                coverImageUrl,
                likeCount,
                favoriteCount,
                commentCount,
                viewCount,
                hotScore,
                createdTime,
                updatedTime,
                activityTime,
                score,
                pinnedValue,
                reasons
        );
    }
}
