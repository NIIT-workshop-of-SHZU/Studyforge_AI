package com.studyforge.system.service.achievement.impl;

import com.studyforge.system.entity.achievement.Achievement;
import com.studyforge.system.entity.achievement.UserAchievement;
import com.studyforge.system.mapper.achievement.AchievementMapper;
import com.studyforge.system.mapper.achievement.UserAchievementMapper;
import com.studyforge.system.service.achievement.AchievementService;
import com.studyforge.system.vo.AchievementVO;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class AchievementServiceImpl implements AchievementService {
    private final AchievementMapper achievementMapper;
    private final UserAchievementMapper userAchievementMapper;

    public AchievementServiceImpl(AchievementMapper achievementMapper, UserAchievementMapper userAchievementMapper) {
        this.achievementMapper = achievementMapper;
        this.userAchievementMapper = userAchievementMapper;
    }

    @Override
    public List<AchievementVO> listAllAchievements(String languageCode) {
        return achievementMapper.selectAll("ACTIVE")
                .stream()
                .map(this::toAchievementVO)
                .toList();
    }

    @Override
    public List<AchievementVO> listUserAchievements(Long userId, String languageCode) {
        List<UserAchievement> userAchievements = userAchievementMapper.selectByUserId(userId);
        Set<Long> unlockedIds = userAchievements.stream()
                .map(UserAchievement::getAchievementId)
                .collect(Collectors.toSet());

        return achievementMapper.selectAll("ACTIVE")
                .stream()
                .map(achievement -> {
                    boolean unlocked = unlockedIds.contains(achievement.getAchievementId());
                    return toAchievementVO(achievement, unlocked);
                })
                .toList();
    }

    @Override
    public int getUserAchievementCount(Long userId) {
        return userAchievementMapper.countByUserId(userId);
    }

    private AchievementVO toAchievementVO(Achievement achievement) {
        return toAchievementVO(achievement, false);
    }

    private AchievementVO toAchievementVO(Achievement achievement, boolean unlocked) {
        return new AchievementVO(
                achievement.getAchievementId(),
                achievement.getAchievementCode(),
                achievement.getName(),
                achievement.getDescription(),
                achievement.getIcon(),
                achievement.getCategory(),
                achievement.getRewardPoints(),
                unlocked
        );
    }
}
