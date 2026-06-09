package com.studyforge.webapi.homepage;

import com.studyforge.common.api.ApiResponse;
import com.studyforge.common.constants.HttpHeaders;
import com.studyforge.system.service.AuthService;
import com.studyforge.system.service.HomepageService;
import com.studyforge.system.vo.CommunityHomepageDesignVO;
import com.studyforge.system.vo.UserHomepageVO;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/community/homepages")
public class CommunityHomepageController {
    private final AuthService authService;
    private final HomepageService homepageService;

    public CommunityHomepageController(AuthService authService, HomepageService homepageService) {
        this.authService = authService;
        this.homepageService = homepageService;
    }

    @GetMapping
    public ApiResponse<List<CommunityHomepageDesignVO>> list(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
                                                             @RequestParam(name = "keyword", required = false) String keyword,
                                                             @RequestParam(name = "limit", defaultValue = "20") int limit) {
        Long viewerId = authService.currentUserId(authorization);
        return ApiResponse.success(homepageService.listCommunityDesigns(viewerId, keyword, limit));
    }

    @GetMapping("/{designId}")
    public ApiResponse<CommunityHomepageDesignVO> detail(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
                                                         @PathVariable("designId") Long designId) {
        Long viewerId = authService.currentUserId(authorization);
        return ApiResponse.success(homepageService.getCommunityDesign(viewerId, designId));
    }

    @PostMapping("/{designId}/clone")
    public ApiResponse<UserHomepageVO> clone(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
                                             @PathVariable("designId") Long designId) {
        Long userId = authService.requireUserId(authorization);
        return ApiResponse.success(homepageService.cloneCommunityDesign(userId, designId));
    }
}
