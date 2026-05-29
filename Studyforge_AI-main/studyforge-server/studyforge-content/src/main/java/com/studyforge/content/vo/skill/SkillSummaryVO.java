package com.studyforge.content.vo.skill;

public record SkillSummaryVO(Long skillId,
                             String skillCode,
                             String name,
                             String description,
                             String coverImageUrl,
                             String difficulty,
                             int learnerCount,
                             double ratingScore,
                             int postCount) {
}
