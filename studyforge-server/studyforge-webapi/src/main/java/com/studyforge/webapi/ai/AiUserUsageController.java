package com.studyforge.webapi.ai;

import com.studyforge.admin.service.AiUserUsageService;
import com.studyforge.admin.vo.AiUserUsageVO;
import com.studyforge.common.api.ApiResponse;
import com.studyforge.system.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 用户 AI 使用统计控制器
 */
@RestController
@RequestMapping("/api/v1/ai")
public class AiUserUsageController {
    
    @Autowired
    private AiUserUsageService userUsageService;
    
    @Autowired
    private AuthService authService;
    
    /**
     * 获取当前用户的 AI 使用统计
     * 
     * @param authorization Authorization header
     * @return 当前用户使用统计
     */
    @GetMapping("/me/usage")
    public ApiResponse<AiUserUsageVO> getMyUsage(
            @RequestHeader("Authorization") String authorization) {
        
        // 获取当前登录用户 ID
        Long userId = authService.requireUserId(authorization);
        
        AiUserUsageVO usage = userUsageService.getUserUsage(userId);
        return ApiResponse.success(usage);
    }
}
