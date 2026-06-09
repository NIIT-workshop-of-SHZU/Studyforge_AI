import { http, unwrap } from '@/api/http';

/**
 * AI统计数据类型定义
 * 安全设计：只包含脱敏后的聚合数据
 */
export interface AiUsageOverview {
  todayTotalCalls: number;        // 今日总调用次数
  todayUniqueUsers: number;       // 今日独立用户数（已脱敏）
  todaySuccessfulCalls: number;   // 今日成功调用数
  todayFailedCalls: number;       // 今日失败调用数
  todaySuccessRate: number;       // 今日成功率（百分比）
  monthTotalCalls: number;        // 本月总调用次数
  estimatedCost: number;          // 预估费用
  featureDistribution: FeatureDistributionItem[];
  cacheHitCount: number;          // 缓存命中次数
  cacheHitRate: number;           // 缓存命中率
}

export interface FeatureDistributionItem {
  featureType: string;    // 功能类型
  callCount: number;      // 调用次数
  percentage: number;     // 占比百分比
}

export interface AiDailyStats {
  statDate: string;         // 统计日期 (YYYY-MM-DD)
  totalCalls: number;       // 总调用次数
  uniqueUsers: number;      // 独立用户数（脱敏）
  successfulCalls: number;  // 成功调用数
  successRate: number;      // 成功率
}

/**
 * 获取AI使用概览
 * 安全措施：
 * 1. 需要管理员token（由http拦截器自动添加）
 * 2. HTTPS传输（生产环境强制）
 * 3. 响应数据已脱敏
 */
export function getAiUsageOverview() {
  return unwrap<AiUsageOverview>(http.get('/admin/ai/stats/overview'));
}

/**
 * 获取调用趋势
 * @param days 天数，后端会限制在1-30天范围内
 */
export function getAiUsageTrend(days: number = 7) {
  // 前端也做参数校验
  if (days < 1) days = 7;
  if (days > 30) days = 30;
  
  return unwrap<AiDailyStats[]>(
    http.get('/admin/ai/stats/trend', {
      params: { days }
    })
  );
}

/**
 * 获取功能分布统计
 */
export function getAiFeatureDistribution() {
  return unwrap<FeatureDistributionItem[]>(
    http.get('/admin/ai/stats/distribution')
  );
}
