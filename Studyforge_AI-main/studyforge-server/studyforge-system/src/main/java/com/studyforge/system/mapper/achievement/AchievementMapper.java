package com.studyforge.system.mapper.achievement;

import com.studyforge.system.entity.achievement.Achievement;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface AchievementMapper {
    Achievement selectById(@Param("achievementId") Long achievementId);

    Achievement selectByCode(@Param("achievementCode") String achievementCode);

    List<Achievement> selectAll(@Param("status") String status);

    List<Achievement> selectByCategory(@Param("category") String category, @Param("status") String status);
}
