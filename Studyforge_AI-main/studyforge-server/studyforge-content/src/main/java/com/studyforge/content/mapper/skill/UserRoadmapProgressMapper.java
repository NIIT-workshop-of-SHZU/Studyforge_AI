package com.studyforge.content.mapper.skill;

import com.studyforge.content.entity.skill.UserRoadmapProgress;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface UserRoadmapProgressMapper {
    UserRoadmapProgress selectByUserAndRoadmap(@Param("userId") Long userId, @Param("roadmapId") Long roadmapId);

    List<UserRoadmapProgress> selectByUserId(@Param("userId") Long userId, @Param("status") String status);

    int insert(UserRoadmapProgress progress);

    int updateById(UserRoadmapProgress progress);

    int deleteByUserAndRoadmap(@Param("userId") Long userId, @Param("roadmapId") Long roadmapId);
}
