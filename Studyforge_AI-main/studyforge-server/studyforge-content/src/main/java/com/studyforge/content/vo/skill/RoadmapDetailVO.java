package com.studyforge.content.vo.skill;

import java.time.LocalDateTime;
import java.util.List;

public record RoadmapDetailVO(Long roadmapId,
                              Long skillId,
                              String skillName,
                              Long authorId,
                              String title,
                              String summary,
                              String difficulty,
                              int estimatedDays,
                              String coverImageUrl,
                              double ratingScore,
                              int learnerCount,
                              int nodeCount,
                              List<RoadmapNodeVO> nodes,
                              LocalDateTime createdTime,
                              LocalDateTime updatedTime) {
}
