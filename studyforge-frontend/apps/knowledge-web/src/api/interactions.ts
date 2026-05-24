import { http, unwrap } from '@/api/http';
import type { CommentItem, PostInteractionState } from '@/types/api';

export function getInteractionState(postId: number | string) {
  return unwrap<PostInteractionState>(http.get(`/posts/${postId}/interaction`));
}

export function toggleLike(postId: number | string) {
  return unwrap<PostInteractionState>(http.post(`/posts/${postId}/likes`));
}

export function toggleFavorite(postId: number | string) {
  return unwrap<PostInteractionState>(http.post(`/posts/${postId}/favorites`));
}

export function recordPostView(postId: number | string) {
  return unwrap<void>(http.post(`/posts/${postId}/views`));
}

export function getComments(postId: number | string) {
  return unwrap<CommentItem[]>(http.get(`/posts/${postId}/comments`));
}

export function createComment(postId: number | string, content: string, languageCode: string) {
  return unwrap<CommentItem>(
    http.post(`/posts/${postId}/comments`, {
      content,
      languageCode
    })
  );
}
