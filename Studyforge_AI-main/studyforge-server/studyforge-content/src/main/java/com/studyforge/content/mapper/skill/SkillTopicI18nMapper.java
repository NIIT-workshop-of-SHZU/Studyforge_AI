package com.studyforge.content.mapper.skill;

import com.studyforge.content.entity.skill.SkillTopicI18n;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface SkillTopicI18nMapper {
    SkillTopicI18n selectBySkillIdAndLanguage(@Param("skillId") Long skillId, @Param("languageCode") String languageCode);

    List<SkillTopicI18n> selectBySkillId(@Param("skillId") Long skillId);

    int insert(SkillTopicI18n skillTopicI18n);

    int updateById(SkillTopicI18n skillTopicI18n);

    int deleteBySkillId(@Param("skillId") Long skillId);
}
