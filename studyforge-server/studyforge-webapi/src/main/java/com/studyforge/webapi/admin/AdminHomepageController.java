package com.studyforge.webapi.admin;

import com.studyforge.common.api.ApiResponse;
import com.studyforge.common.constants.HttpHeaders;
import com.studyforge.common.enums.RoleType;
import com.studyforge.common.exception.BizException;
import com.studyforge.common.exception.ErrorCode;
import com.studyforge.system.dto.HomepageReviewRequest;
import com.studyforge.system.entity.User;
import com.studyforge.system.service.AuthService;
import com.studyforge.system.service.HomepageService;
import com.studyforge.system.vo.AdminHomepageReviewVO;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/homepages")
public class AdminHomepageController {
    private final AuthService authService;
    private final HomepageService homepageService;

    public AdminHomepageController(AuthService authService, HomepageService homepageService) {
        this.authService = authService;
        this.homepageService = homepageService;
    }

    @GetMapping("/reviews")
    public ApiResponse<List<AdminHomepageReviewVO>> reviews(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
                                                            @RequestParam(name = "status", defaultValue = "PENDING_REVIEW") String status,
                                                            @RequestParam(name = "limit", defaultValue = "30") int limit) {
        requireAdminUser(authorization);
        return ApiResponse.success(homepageService.listReviews(status, limit));
    }

    @PostMapping("/{targetId}/review")
    public ApiResponse<AdminHomepageReviewVO> review(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
                                                     @PathVariable("targetId") Long targetId,
                                                     @RequestBody HomepageReviewRequest request) {
        User admin = requireAdminUser(authorization);
        return ApiResponse.success(homepageService.review(admin.getUserId(), targetId, request));
    }

    private User requireAdminUser(String authorization) {
        User admin = authService.requireUser(authorization);
        if (!RoleType.ADMIN.equals(admin.getRole())) {
            throw new BizException(ErrorCode.FORBIDDEN, "admin permission is required");
        }
        return admin;
    }
}
