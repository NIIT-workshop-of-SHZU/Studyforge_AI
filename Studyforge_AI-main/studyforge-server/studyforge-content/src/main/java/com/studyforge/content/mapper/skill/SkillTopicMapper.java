package com.studyforge.content.mapper.skill;

import com.studyforge.content.entity.skill.SkillTopic;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface SkillTopicMapper {
    SkillTopic selectById(@Param("skillId") Long skillId);

    SkillTopic selectByCode(@Param("skillCode") String skillCode);

    List<SkillTopic> selectAll(@Param("status") String status);

    List<SkillTopic> selectByDifficulty(@Param("difficulty") String difficulty, @Param("status") String status);

    int insert(SkillTopic skillTopic);

    int updateById(SkillTopic skillTopic);

    int incrementLearnerCount(@Param("skillId") Long skillId, @Param("delta") int delta);

    int incrementPostCount(@Param("skillId") Long skillId, @Param("delta") int delta);
}
