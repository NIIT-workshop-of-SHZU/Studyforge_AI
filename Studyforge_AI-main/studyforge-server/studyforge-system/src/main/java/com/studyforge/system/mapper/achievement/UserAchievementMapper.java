package com.studyforge.system.mapper.achievement;

import com.studyforge.system.entity.achievement.UserAchievement;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface UserAchievementMapper {
    List<UserAchievement> selectByUserId(@Param("userId") Long userId);

    UserAchievement selectByUserAndAchievement(@Param("userId") Long userId, @Param("achievementId") Long achievementId);

    int insert(UserAchievement userAchievement);

    int countByUserId(@Param("userId") Long userId);
}
