package com.studyforge.webapi.admin;

import com.studyforge.admin.service.AiUsageStatsService;
import com.studyforge.admin.vo.AiDailyStatsVO;
import com.studyforge.admin.vo.AiUsageOverviewVO;
import com.studyforge.common.api.ApiResponse;
import com.studyforge.common.constants.HttpHeaders;
import com.studyforge.common.enums.RoleType;
import com.studyforge.system.entity.User;
import com.studyforge.system.service.AuthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * AI使用统计控制器
 * 安全措施：
 * 1. 严格的管理员权限校验
 * 2. 参数范围限制
 * 3. 响应数据脱敏
 * 4. 访问日志记录
 */
@RestController
@RequestMapping("/api/v1/admin/ai/stats")
public class AdminAiStatsController {
    
    private final AiUsageStatsService aiUsageStatsService;
    private final AuthService authService;
    
    public AdminAiStatsController(AiUsageStatsService aiUsageStatsService, 
                                  AuthService authService) {
        this.aiUsageStatsService = aiUsageStatsService;
        this.authService = authService;
    }
    
    /**
     * 获取AI使用概览
     * 安全级别：高 - 需要管理员权限
     */
    @GetMapping("/overview")
    public ApiResponse<AiUsageOverviewVO> getOverview(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        // 严格的权限校验
        authService.requireAdmin(authorization);
        
        return ApiResponse.success(aiUsageStatsService.getUsageOverview());
    }
    
    /**
     * 获取调用趋势
     * 安全级别：高 - 需要管理员权限，且限制查询范围
     */
    @GetMapping("/trend")
    public ApiResponse<List<AiDailyStatsVO>> getTrend(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @RequestParam(name = "days", defaultValue = "7") int days) {
        // 严格的权限校验
        authService.requireAdmin(authorization);
        
        // 参数安全校验：限制查询范围
        if (days < 1 || days > 30) {
            days = 7; // 超出范围则使用默认值
        }
        
        return ApiResponse.success(aiUsageStatsService.getUsageTrend(days));
    }
    
    /**
     * 获取功能分布
     * 安全级别：高 - 需要管理员权限
     */
    @GetMapping("/distribution")
    public ApiResponse<List<AiUsageOverviewVO.FeatureDistributionItem>> getDistribution(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        // 严格的权限校验
        authService.requireAdmin(authorization);
        
        return ApiResponse.success(aiUsageStatsService.getFeatureDistribution());
    }
}
