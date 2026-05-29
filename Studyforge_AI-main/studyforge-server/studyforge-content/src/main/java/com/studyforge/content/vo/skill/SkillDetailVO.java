package com.studyforge.content.vo.skill;

import java.time.LocalDateTime;
import java.util.List;

public record SkillDetailVO(Long skillId,
                            String skillCode,
                            String name,
                            String description,
                            String coverImageUrl,
                            String difficulty,
                            int learnerCount,
                            double ratingScore,
                            int postCount,
                            List<RoadmapSummaryVO> roadmaps,
                            LocalDateTime createdTime,
                            LocalDateTime updatedTime) {
}
