package com.studyforge.webapi.skill;

import com.studyforge.common.api.ApiResponse;
import com.studyforge.common.constants.HttpHeaders;
import com.studyforge.content.service.skill.SkillService;
import com.studyforge.content.vo.skill.RoadmapDetailVO;
import com.studyforge.content.vo.skill.RoadmapSummaryVO;
import com.studyforge.content.vo.skill.SkillDetailVO;
import com.studyforge.content.vo.skill.SkillSummaryVO;
import com.studyforge.content.vo.skill.UserRoadmapProgressVO;
import com.studyforge.system.service.AuthService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/skills")
public class SkillController {
    private final SkillService skillService;
    private final AuthService authService;

    public SkillController(SkillService skillService, AuthService authService) {
        this.skillService = skillService;
        this.authService = authService;
    }

    @GetMapping
    public ApiResponse<List<SkillSummaryVO>> listSkills(
            @RequestParam(name = "languageCode", defaultValue = "zh_CN") String languageCode,
            @RequestParam(name = "difficulty", required = false) String difficulty,
            @RequestParam(name = "limit", defaultValue = "20") int limit) {
        return ApiResponse.success(skillService.listSkills(languageCode, difficulty, limit));
    }

    @GetMapping("/{skillCode}")
    public ApiResponse<SkillDetailVO> getSkillDetail(
            @PathVariable("skillCode") String skillCode,
            @RequestParam(name = "languageCode", defaultValue = "zh_CN") String languageCode) {
        return ApiResponse.success(skillService.getSkillDetail(skillCode, languageCode));
    }

    @GetMapping("/{skillCode}/roadmaps")
    public ApiResponse<List<RoadmapSummaryVO>> listRoadmapsBySkill(
            @PathVariable("skillCode") String skillCode,
            @RequestParam(name = "languageCode", defaultValue = "zh_CN") String languageCode,
            @RequestParam(name = "limit", defaultValue = "20") int limit) {
        SkillDetailVO skill = skillService.getSkillDetail(skillCode, languageCode);
        return ApiResponse.success(skillService.listRoadmapsBySkill(skill.skillId(), languageCode, limit));
    }

    @GetMapping("/roadmaps/popular")
    public ApiResponse<List<RoadmapSummaryVO>> listPopularRoadmaps(
            @RequestParam(name = "languageCode", defaultValue = "zh_CN") String languageCode,
            @RequestParam(name = "limit", defaultValue = "10") int limit) {
        return ApiResponse.success(skillService.listPopularRoadmaps(languageCode, limit));
    }

    @GetMapping("/roadmaps/{roadmapId}")
    public ApiResponse<RoadmapDetailVO> getRoadmapDetail(
            @PathVariable("roadmapId") Long roadmapId,
            @RequestParam(name = "languageCode", defaultValue = "zh_CN") String languageCode) {
        return ApiResponse.success(skillService.getRoadmapDetail(roadmapId, languageCode));
    }

    @PostMapping("/roadmaps/{roadmapId}/start")
    public ApiResponse<Long> startRoadmap(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @PathVariable("roadmapId") Long roadmapId) {
        Long userId = authService.requireUserId(authorization);
        return ApiResponse.success("started", skillService.startRoadmap(userId, roadmapId));
    }

    @PostMapping("/roadmaps/nodes/{nodeId}/complete")
    public ApiResponse<UserRoadmapProgressVO> completeNode(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @PathVariable("nodeId") Long nodeId) {
        Long userId = authService.requireUserId(authorization);
        return ApiResponse.success(skillService.completeNode(userId, nodeId));
    }

    @GetMapping("/roadmaps/{roadmapId}/progress")
    public ApiResponse<UserRoadmapProgressVO> getProgress(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @PathVariable("roadmapId") Long roadmapId) {
        Long userId = authService.requireUserId(authorization);
        return ApiResponse.success(skillService.getProgress(userId, roadmapId));
    }

    @GetMapping("/me/roadmaps")
    public ApiResponse<List<UserRoadmapProgressVO>> listMyRoadmaps(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @RequestParam(name = "status", required = false) String status) {
        Long userId = authService.requireUserId(authorization);
        return ApiResponse.success(skillService.listUserProgress(userId, status));
    }
}
