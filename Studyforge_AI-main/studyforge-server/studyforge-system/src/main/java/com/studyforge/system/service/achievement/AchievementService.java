package com.studyforge.system.service.achievement;

import com.studyforge.system.vo.AchievementVO;
import java.util.List;

public interface AchievementService {
    List<AchievementVO> listAllAchievements(String languageCode);

    List<AchievementVO> listUserAchievements(Long userId, String languageCode);

    int getUserAchievementCount(Long userId);
}
