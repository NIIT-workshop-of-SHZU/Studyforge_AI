export interface ApiEnvelope<T> {
  code: number;
  message: string;
  data: T;
  requestId?: string | null;
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
  coverImageUrl: string | null;
  likeCount: number;
  favoriteCount: number;
  commentCount: number;
  viewCount: number;
  hotScore: number;
}

export interface PostDetail extends PostSummary {
  authorId: number;
  content: string;
  contentFormat: 'MARKDOWN' | 'TEXT' | string;
}

export interface CreatePostRequest {
  categoryId: number;
  originalLanguage: string;
  coverImageUrl?: string | null;
  title: string;
  summary: string;
  content: string;
}

export interface UploadedFile {
  fileId: number;
  originalFilename: string;
  filename: string;
  url: string;
  contentType: string | null;
  size: number;
}

export interface PostInteractionState {
  liked: boolean;
  favorited: boolean;
  likeCount: number;
  favoriteCount: number;
  commentCount: number;
  viewCount: number;
}

export interface CommentItem {
  commentId: number;
  postId: number;
  userId: number;
  languageCode: string;
  content: string;
  createdTime: string;
}

export interface AiResult {
  type: string;
  languageCode: string;
  text: string;
}

export interface AiLogItem {
  logId: number;
  postId: number;
  aiType: string;
  responseText: string;
  success: number;
  createdTime: string;
}

export interface VoiceResult {
  audioDataUrl: string;
  format: string;
}

export interface HelpRequest {
  helpId: number;
  userId: number;
  title: string;
  description: string;
  categoryId: number | null;
  status: string;
  rewardPoints: number;
  createdTime: string;
}

export interface HelpAnswer {
  answerId: number;
  helpId: number;
  userId: number;
  content: string;
  accepted: number;
  createdTime: string;
}

export interface TopicCategory {
  code: string;
  name: string;
  description: string;
  accent: string;
}
