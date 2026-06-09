package com.studyforge.webapi.user;

import com.studyforge.common.api.ApiResponse;
import com.studyforge.common.constants.HttpHeaders;
import com.studyforge.system.dto.HomepageDraftRequest;
import com.studyforge.system.dto.PublishHomepageRequest;
import com.studyforge.system.service.AuthService;
import com.studyforge.system.service.HomepageService;
import com.studyforge.system.vo.CommunityHomepageDesignVO;
import com.studyforge.system.vo.UserHomepageVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserHomepageController {
    private final AuthService authService;
    private final HomepageService homepageService;

    public UserHomepageController(AuthService authService, HomepageService homepageService) {
        this.authService = authService;
        this.homepageService = homepageService;
    }

    @GetMapping("/me/homepage")
    public ApiResponse<UserHomepageVO> myHomepage(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        Long userId = authService.requireUserId(authorization);
        return ApiResponse.success(homepageService.getMyHomepage(userId));
    }

    @GetMapping("/{userId}/homepage")
    public ApiResponse<UserHomepageVO> userHomepage(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
                                                    @PathVariable("userId") Long userId) {
        Long viewerId = authService.currentUserId(authorization);
        return ApiResponse.success(homepageService.getUserHomepage(viewerId, userId));
    }

    @PutMapping("/me/homepage/draft")
    public ApiResponse<UserHomepageVO> saveDraft(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
                                                 @RequestBody HomepageDraftRequest request) {
        Long userId = authService.requireUserId(authorization);
        return ApiResponse.success(homepageService.saveDraft(userId, request));
    }

    @PostMapping("/me/homepage/submit-review")
    public ApiResponse<UserHomepageVO> submitReview(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        Long userId = authService.requireUserId(authorization);
        return ApiResponse.success(homepageService.submitReview(userId));
    }

    @PostMapping("/me/homepage/publish-community")
    public ApiResponse<CommunityHomepageDesignVO> publishCommunity(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
                                                                   @RequestBody PublishHomepageRequest request) {
        Long userId = authService.requireUserId(authorization);
        return ApiResponse.success(homepageService.publishCommunity(userId, request));
    }
}
