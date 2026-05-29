package com.studyforge.webapi.achievement;

import com.studyforge.common.api.ApiResponse;
import com.studyforge.common.constants.HttpHeaders;
import com.studyforge.system.service.achievement.AchievementService;
import com.studyforge.system.service.AuthService;
import com.studyforge.system.vo.AchievementVO;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/achievements")
public class AchievementController {
    private final AchievementService achievementService;
    private final AuthService authService;

    public AchievementController(AchievementService achievementService, AuthService authService) {
        this.achievementService = achievementService;
        this.authService = authService;
    }

    @GetMapping
    public ApiResponse<List<AchievementVO>> listAllAchievements(
            @RequestParam(name = "languageCode", defaultValue = "zh_CN") String languageCode) {
        return ApiResponse.success(achievementService.listAllAchievements(languageCode));
    }

    @GetMapping("/me")
    public ApiResponse<List<AchievementVO>> listMyAchievements(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @RequestParam(name = "languageCode", defaultValue = "zh_CN") String languageCode) {
        Long userId = authService.requireUserId(authorization);
        return ApiResponse.success(achievementService.listUserAchievements(userId, languageCode));
    }

    @GetMapping("/me/count")
    public ApiResponse<Integer> getMyAchievementCount(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        Long userId = authService.requireUserId(authorization);
        return ApiResponse.success(achievementService.getUserAchievementCount(userId));
    }
}
