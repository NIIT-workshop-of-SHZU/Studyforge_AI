package com.studyforge.admin.service;

import com.studyforge.admin.vo.AiDailyStatsVO;
import com.studyforge.admin.vo.AiUsageOverviewVO;
import java.util.List;

/**
 * AI使用统计服务接口
 * 安全设计：
 * 1. 只返回聚合统计数据
 * 2. 不暴露任何用户个人信息
 * 3. 不暴露API密钥等敏感配置
 */
public interface AiUsageStatsService {
    
    /**
     * 获取AI使用概览（今日+本月统计）
     * @return 脱敏后的统计数据
     */
    AiUsageOverviewVO getUsageOverview();
    
    /**
     * 获取调用趋势（最近N天）
     * @param days 天数，最大限制30天（防止大数据量查询）
     * @return 每日统计数据列表
     */
    List<AiDailyStatsVO> getUsageTrend(int days);
    
    /**
     * 获取功能分布统计（最近30天）
     * @return 各功能调用分布
     */
    List<AiUsageOverviewVO.FeatureDistributionItem> getFeatureDistribution();
}
