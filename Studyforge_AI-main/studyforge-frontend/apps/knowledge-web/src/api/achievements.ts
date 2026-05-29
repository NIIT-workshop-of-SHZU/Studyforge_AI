import { http, unwrap } from '@/api/http';

export interface Achievement {
  achievementId: number;
  achievementCode: string;
  name: string;
  description: string;
  icon: string;
  category: string;
  rewardPoints: number;
  unlocked: boolean;
}

export function getAllAchievements(languageCode: string) {
  return unwrap<Achievement[]>(
    http.get('/achievements', {
      params: { languageCode }
    })
  );
}

export function getMyAchievements(languageCode: string) {
  return unwrap<Achievement[]>(
    http.get('/achievements/me', {
      params: { languageCode }
    })
  );
}

export function getMyAchievementCount() {
  return unwrap<number>(http.get('/achievements/me/count'));
}
