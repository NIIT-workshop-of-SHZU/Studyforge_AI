package com.studyforge.system.vo;

import java.time.LocalDateTime;

public record UserHomepageVO(Long homepageId,
                             Long userId,
                             String templateType,
                             String layoutMode,
                             String themeConfig,
                             String customCodeDraft,
                             String mediaLayoutDraft,
                             String publishedTemplateType,
                             String publishedLayoutMode,
                             String publishedThemeConfig,
                             String publishedCustomCode,
                             String publishedMediaLayout,
                             int publishedVersion,
                             String reviewStatus,
                             LocalDateTime submittedAt,
                             LocalDateTime reviewDeadlineAt,
                             String reviewComment,
                             Long communityTemplateId,
                             boolean self) {
}
