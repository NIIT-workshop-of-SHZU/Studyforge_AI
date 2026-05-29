package com.studyforge.system.vo;

public record AchievementVO(Long achievementId,
                            String achievementCode,
                            String name,
                            String description,
                            String icon,
                            String category,
                            int rewardPoints,
                            boolean unlocked) {
}
