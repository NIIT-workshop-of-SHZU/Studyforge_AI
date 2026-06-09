import { http, unwrap } from './http';

export interface AiUserUsage {
  userId: number;
  username: string;
  displayName: string;
  totalCalls: number;
  totalPromptTokens: number;
  totalCompletionTokens: number;
  totalTokens: number;
  totalCostYuan: number;
  summaryCalls: number;
  reviewCardCalls: number;
  questionCalls: number;
  markdownFormatCalls: number;
  successfulCalls: number;
  failedCalls: number;
  successRate: number;
}

/**
 * 获取当前用户的 AI 使用统计
 */
export function getMyAiUsage() {
  return unwrap<AiUserUsage>(http.get('/ai/me/usage'));
}
