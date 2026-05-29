package com.studyforge.content.vo.skill;

public record RoadmapSummaryVO(Long roadmapId,
                               Long skillId,
                               String skillName,
                               String title,
                               String summary,
                               String difficulty,
                               int estimatedDays,
                               String coverImageUrl,
                               double ratingScore,
                               int learnerCount,
                               int nodeCount) {
}
