import { http, unwrap } from '@/api/http';
import type { AiLogItem, AiResult } from '@/types/api';

export function generateSummary(postId: number | string, languageCode: string) {
  return unwrap<AiResult>(http.post(`/ai/posts/${postId}/summary`, { languageCode }));
}

export function generateReviewCards(postId: number | string, languageCode: string) {
  return unwrap<AiResult>(http.post(`/ai/posts/${postId}/review-cards`, { languageCode }));
}

export function askPostQuestion(postId: number | string, question: string, answerLanguage: string) {
  return unwrap<AiResult>(http.post(`/ai/posts/${postId}/questions`, { question, answerLanguage }));
}

export function formatMarkdown(content: string, languageCode: string) {
  return unwrap<AiResult>(http.post('/ai/markdown/format', { content, languageCode }));
}

export function getMyReviewCards() {
  return unwrap<AiLogItem[]>(http.get('/ai/me/review-cards', { params: { limit: 20 } }));
}
