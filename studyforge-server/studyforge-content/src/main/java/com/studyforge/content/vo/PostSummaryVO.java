package com.studyforge.content.vo;

public record PostSummaryVO(Long postId,
                            String title,
                            String summary,
                            String languageCode,
                            String categoryCode,
                            String coverImageUrl,
                            int likeCount,
                            int favoriteCount,
                            int commentCount,
                            int viewCount,
                            double hotScore) {
}
