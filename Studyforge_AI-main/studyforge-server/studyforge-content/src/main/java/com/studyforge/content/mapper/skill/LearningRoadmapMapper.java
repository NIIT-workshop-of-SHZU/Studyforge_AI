package com.studyforge.content.mapper.skill;

import com.studyforge.content.entity.skill.LearningRoadmap;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface LearningRoadmapMapper {
    LearningRoadmap selectById(@Param("roadmapId") Long roadmapId);

    List<LearningRoadmap> selectBySkillId(@Param("skillId") Long skillId, @Param("status") String status);

    List<LearningRoadmap> selectByAuthorId(@Param("authorId") Long authorId, @Param("status") String status);

    List<LearningRoadmap> selectPopular(@Param("limit") int limit, @Param("status") String status);

    int insert(LearningRoadmap learningRoadmap);

    int updateById(LearningRoadmap learningRoadmap);

    int incrementLearnerCount(@Param("roadmapId") Long roadmapId, @Param("delta") int delta);

    int incrementNodeCount(@Param("roadmapId") Long roadmapId, @Param("delta") int delta);
}
