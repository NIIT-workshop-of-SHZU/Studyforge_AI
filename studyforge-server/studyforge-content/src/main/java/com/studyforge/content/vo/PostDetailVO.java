package com.studyforge.content.vo;

import java.time.LocalDateTime;
import java.util.List;

public record PostDetailVO(Long postId,
                           Long authorId,
                           String authorName,
                           String authorAvatarUrl,
                           String title,
                           String summary,
                           String content,
                           String languageCode,
                           String originalLanguage,
                           List<String> availableLanguages,
                           String categoryCode,
                           String coverImageUrl,
                           String contentFormat,
                           int likeCount,
                           int favoriteCount,
                           int commentCount,
                           int viewCount,
                           double hotScore,
                           LocalDateTime createdTime,
                           LocalDateTime updatedTime,
                           List<String> semanticTags) {
    public PostDetailVO withSemanticTags(List<String> tags) {
        return new PostDetailVO(
                postId,
                authorId,
                authorName,
                authorAvatarUrl,
                title,
                summary,
                content,
                languageCode,
                originalLanguage,
                availableLanguages,
                categoryCode,
                coverImageUrl,
                contentFormat,
                likeCount,
                favoriteCount,
                commentCount,
                viewCount,
                hotScore,
                createdTime,
                updatedTime,
                tags == null ? List.of() : tags
        );
    }
}
