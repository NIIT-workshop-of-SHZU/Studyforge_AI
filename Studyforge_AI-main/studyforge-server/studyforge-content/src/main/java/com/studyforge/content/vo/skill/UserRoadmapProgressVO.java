package com.studyforge.content.vo.skill;

import java.time.LocalDateTime;
import java.util.List;

public record UserRoadmapProgressVO(Long progressId,
                                    Long userId,
                                    Long roadmapId,
                                    String roadmapTitle,
                                    Long currentNodeId,
                                    int completedNodes,
                                    int totalNodes,
                                    double progressPercent,
                                    String status,
                                    List<RoadmapNodeVO> nodes,
                                    LocalDateTime startedTime,
                                    LocalDateTime updatedTime) {
}
