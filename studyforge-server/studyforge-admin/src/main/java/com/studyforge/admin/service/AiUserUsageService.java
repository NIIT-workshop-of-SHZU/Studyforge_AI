package com.studyforge.admin.service;

import com.studyforge.admin.vo.AiUserUsageVO;
import java.util.List;

/**
 * 用户 AI 使用统计服务
 */
public interface AiUserUsageService {
    
    /**
     * 获取指定用户的 AI 使用统计
     * 
     * @param userId 用户 ID
     * @return 用户使用统计
     */
    AiUserUsageVO getUserUsage(Long userId);
    
    /**
     * 获取所有用户的 AI 使用量排名
     * 
     * @param limit 返回数量限制
     * @return 用户用量排名列表
     */
    List<AiUserUsageVO> getUserUsageRanking(int limit);
}
