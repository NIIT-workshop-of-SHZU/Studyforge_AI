package com.studyforge.content.vo.skill;

public record RoadmapNodeVO(Long nodeId,
                            int sortNo,
                            String title,
                            String description,
                            int estimatedMinutes,
                            Long relatedPostId) {
}
