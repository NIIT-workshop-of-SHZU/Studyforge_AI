import { http, unwrap } from '@/api/http';

export interface SkillSummary {
  skillId: number;
  skillCode: string;
  name: string;
  description: string;
  coverImageUrl: string | null;
  difficulty: string;
  learnerCount: number;
  ratingScore: number;
  postCount: number;
}

export interface SkillDetail extends SkillSummary {
  roadmaps: RoadmapSummary[];
  createdTime: string | number[] | null;
  updatedTime: string | number[] | null;
}

export interface RoadmapSummary {
  roadmapId: number;
  skillId: number;
  skillName: string;
  title: string;
  summary: string;
  difficulty: string;
  estimatedDays: number;
  coverImageUrl: string | null;
  ratingScore: number;
  learnerCount: number;
  nodeCount: number;
}

export interface RoadmapNode {
  nodeId: number;
  sortNo: number;
  title: string;
  description: string;
  estimatedMinutes: number;
  relatedPostId: number | null;
}

export interface RoadmapDetail extends RoadmapSummary {
  authorId: number;
  nodes: RoadmapNode[];
  createdTime: string | number[] | null;
  updatedTime: string | number[] | null;
}

export interface UserRoadmapProgress {
  progressId: number;
  userId: number;
  roadmapId: number;
  roadmapTitle: string;
  currentNodeId: number | null;
  completedNodes: number;
  totalNodes: number;
  progressPercent: number;
  status: string;
  nodes: RoadmapNode[];
  startedTime: string | number[] | null;
  updatedTime: string | number[] | null;
}

export function getSkills(languageCode: string, difficulty?: string, limit?: number) {
  return unwrap<SkillSummary[]>(
    http.get('/skills', {
      params: {
        languageCode,
        difficulty,
        limit: limit ?? 20
      }
    })
  );
}

export function getSkillDetail(skillCode: string, languageCode: string) {
  return unwrap<SkillDetail>(
    http.get(`/skills/${skillCode}`, {
      params: { languageCode }
    })
  );
}

export function getSkillRoadmaps(skillCode: string, languageCode: string, limit?: number) {
  return unwrap<RoadmapSummary[]>(
    http.get(`/skills/${skillCode}/roadmaps`, {
      params: {
        languageCode,
        limit: limit ?? 20
      }
    })
  );
}

export function getPopularRoadmaps(languageCode: string, limit?: number) {
  return unwrap<RoadmapSummary[]>(
    http.get('/skills/roadmaps/popular', {
      params: {
        languageCode,
        limit: limit ?? 10
      }
    })
  );
}

export function getRoadmapDetail(roadmapId: number | string, languageCode: string) {
  return unwrap<RoadmapDetail>(
    http.get(`/skills/roadmaps/${roadmapId}`, {
      params: { languageCode }
    })
  );
}

export function startRoadmap(roadmapId: number | string) {
  return unwrap<number>(http.post(`/skills/roadmaps/${roadmapId}/start`));
}

export function completeNode(nodeId: number | string) {
  return unwrap<UserRoadmapProgress>(http.post(`/skills/roadmaps/nodes/${nodeId}/complete`));
}

export function getRoadmapProgress(roadmapId: number | string) {
  return unwrap<UserRoadmapProgress>(http.get(`/skills/roadmaps/${roadmapId}/progress`));
}

export function getMyRoadmaps(status?: string) {
  return unwrap<UserRoadmapProgress[]>(
    http.get('/skills/me/roadmaps', {
      params: { status }
    })
  );
}
