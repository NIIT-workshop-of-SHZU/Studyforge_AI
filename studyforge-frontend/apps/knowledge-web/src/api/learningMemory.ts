import { http, unwrap } from '@/api/http';
import type { InterestTag, LearningMemory } from '@/types/api';

export interface UpdateLearningMemoryPayload {
  memoryMd?: string;
  interestTags?: InterestTag[];
  aiMemoryEnabled?: boolean;
}

export function getLearningMemory() {
  return unwrap<LearningMemory>(http.get('/users/me/learning-memory'));
}

export function updateLearningMemory(payload: UpdateLearningMemoryPayload, languageCode: string) {
  return unwrap<LearningMemory>(
    http.put('/users/me/learning-memory', payload, {
      params: { languageCode }
    })
  );
}

export function refreshLearningMemory(languageCode: string) {
  return unwrap<LearningMemory>(
    http.post('/users/me/learning-memory/refresh', null, {
      params: { languageCode }
    })
  );
}
