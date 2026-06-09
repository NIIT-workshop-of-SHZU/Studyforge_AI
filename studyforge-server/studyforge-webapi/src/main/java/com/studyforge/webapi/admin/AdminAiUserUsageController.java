package com.studyforge.webapi.admin;

import com.studyforge.admin.service.AiUserUsageService;
import com.studyforge.admin.vo.AiUserUsageVO;
import com.studyforge.common.api.ApiResponse;
import com.studyforge.system.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理员 - 用户 AI 使用统计控制器
 */
@RestController
@RequestMapping("/api/v1/admin/ai/users")
public class AdminAiUserUsageController {
    
    @Autowired
    private AiUserUsageService userUsageService;
    
    @Autowired
    private AuthService authService;
    
    /**
     * 获取指定用户的 AI 使用统计（需要管理员权限）
     * 
     * @param authorization Authorization header
     * @param userId 用户 ID
     * @return 用户使用统计
     */
    @GetMapping("/{userId}/usage")
    public ApiResponse<AiUserUsageVO> getUserUsage(
            @RequestHeader("Authorization") String authorization,
            @PathVariable("userId") Long userId) {
        
        // 验证管理员权限
        authService.requireAdmin(authorization);
        
        AiUserUsageVO usage = userUsageService.getUserUsage(userId);
        return ApiResponse.success(usage);
    }
    
    /**
     * 获取用户 AI 使用量排名（需要管理员权限）
     * 
     * @param authorization Authorization header
     * @param limit 返回数量限制（默认 50，最大 100）
     * @return 用户用量排名列表
     */
    @GetMapping("/usage-ranking")
    public ApiResponse<List<AiUserUsageVO>> getUserUsageRanking(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(value = "limit", defaultValue = "50") int limit) {
        
        // 验证管理员权限
        authService.requireAdmin(authorization);
        
        List<AiUserUsageVO> ranking = userUsageService.getUserUsageRanking(limit);
        return ApiResponse.success(ranking);
    }
}
