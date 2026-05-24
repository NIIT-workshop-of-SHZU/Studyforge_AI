export interface ApiEnvelope<T> {
  code: number;
  message: string;
  data: T;
  requestId?: string | null;
}

export interface HealthStatus {
  service: string;
  status: string;
}

export interface LoginRequest {
  account: string;
  password: string;
}

export interface LoginSession {
  accessToken: string;
  userId: number;
  username: string;
  role: 'USER' | 'ADMIN' | string;
}

export interface PostSummary {
  postId: number;
  title: string;
  summary: string;
  languageCode: string;
  categoryCode: string;
  likeCount: number;
  favoriteCount: number;
  commentCount: number;
  viewCount: number;
  hotScore: number;
}

export interface PostDetail extends PostSummary {
  authorId: number;
  content: string;
}

export interface IntegrationSetting {
  settingKey: string;
  settingValue: string;
  secretFlag: number;
  updatedBy: number | null;
  createdTime: string | null;
  updatedTime: string | null;
}
