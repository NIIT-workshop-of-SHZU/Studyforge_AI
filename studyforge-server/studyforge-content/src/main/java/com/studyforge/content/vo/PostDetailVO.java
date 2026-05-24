package com.studyforge.content.vo;

public record PostDetailVO(Long postId,
                           Long authorId,
                           String title,
                           String summary,
                           String content,
                           String languageCode,
                           String categoryCode,
                           String coverImageUrl,
                           String contentFormat,
                           int likeCount,
                           int favoriteCount,
                           int commentCount,
                           int viewCount,
                           double hotScore) {
}
