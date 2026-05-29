package com.studyforge.content.service.skill;

import com.studyforge.content.vo.skill.SkillDetailVO;
import com.studyforge.content.vo.skill.SkillSummaryVO;
import com.studyforge.content.vo.skill.RoadmapDetailVO;
import com.studyforge.content.vo.skill.RoadmapSummaryVO;
import com.studyforge.content.vo.skill.UserRoadmapProgressVO;
import java.util.List;

public interface SkillService {
    List<SkillSummaryVO> listSkills(String languageCode, String difficulty, int limit);

    SkillDetailVO getSkillDetail(String skillCode, String languageCode);

    List<RoadmapSummaryVO> listRoadmapsBySkill(Long skillId, String languageCode, int limit);

    List<RoadmapSummaryVO> listPopularRoadmaps(String languageCode, int limit);

    RoadmapDetailVO getRoadmapDetail(Long roadmapId, String languageCode);

    Long startRoadmap(Long userId, Long roadmapId);

    UserRoadmapProgressVO completeNode(Long userId, Long nodeId);

    UserRoadmapProgressVO getProgress(Long userId, Long roadmapId);

    List<UserRoadmapProgressVO> listUserProgress(Long userId, String status);
}
