package com.studyforge.interaction.vo;

import java.time.LocalDateTime;

public record CommentVO(Long commentId,
                        Long postId,
                        Long userId,
                        String languageCode,
                        String content,
                        LocalDateTime createdTime) {
}
