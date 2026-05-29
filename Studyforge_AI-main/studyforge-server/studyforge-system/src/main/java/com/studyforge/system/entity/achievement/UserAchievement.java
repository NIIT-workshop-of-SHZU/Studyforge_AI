package com.studyforge.system.entity.achievement;

import java.time.LocalDateTime;

public class UserAchievement {
    private Long id;
    private Long userId;
    private Long achievementId;
    private LocalDateTime unlockedTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getAchievementId() {
        return achievementId;
    }

    public void setAchievementId(Long achievementId) {
        this.achievementId = achievementId;
    }

    public LocalDateTime getUnlockedTime() {
        return unlockedTime;
    }

    public void setUnlockedTime(LocalDateTime unlockedTime) {
        this.unlockedTime = unlockedTime;
    }
}
