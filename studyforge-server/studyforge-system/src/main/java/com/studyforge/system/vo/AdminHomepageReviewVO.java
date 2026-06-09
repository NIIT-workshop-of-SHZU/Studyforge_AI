package com.studyforge.system.vo;

import java.time.LocalDateTime;

public record AdminHomepageReviewVO(String targetType,
                                    Long targetId,
                                    Long ownerId,
                                    String ownerName,
                                    String ownerAvatarUrl,
                                    String title,
                                    String summary,
                                    String templateType,
                                    String layoutMode,
                                    String themeConfig,
                                    String customCode,
                                    String mediaLayout,
                                    String reviewStatus,
                                    LocalDateTime submittedAt,
                                    LocalDateTime reviewDeadlineAt,
                                    String reviewComment,
                                    boolean overdue) {
}
