import { http, unwrap } from '@/api/http';
import type { FavoriteCollection, PostSummary } from '@/types/api';

export interface CreateCollectionPayload {
  name: string;
  description?: string;
  visibility?: 'PUBLIC' | 'PRIVATE';
}

export function getMyCollections() {
  return unwrap<FavoriteCollection[]>(http.get('/collections/me'));
}

export function createCollection(payload: CreateCollectionPayload) {
  return unwrap<FavoriteCollection>(http.post('/collections', payload));
}

export function getCollectionPosts(
  collectionId: number | string,
  languageCode: string,
  options?: { sort?: 'importance' | 'recent'; tag?: string }
) {
  return unwrap<PostSummary[]>(
    http.get(`/collections/${collectionId}/posts`, {
      params: {
        languageCode,
        sort: options?.sort ?? 'importance',
        tag: options?.tag || undefined,
        limit: 30
      }
    })
  );
}

export function patchCollectionPost(
  collectionId: number | string,
  postId: number | string,
  languageCode: string,
  payload: { pinned?: boolean }
) {
  return unwrap<void>(
    http.patch(`/collections/${collectionId}/posts/${postId}`, payload, {
      params: { languageCode }
    })
  );
}

export function addPostToCollection(collectionId: number | string, postId: number | string) {
  return unwrap<FavoriteCollection>(http.post(`/collections/${collectionId}/posts/${postId}`));
}

export function removePostFromCollection(collectionId: number | string, postId: number | string) {
  return unwrap<FavoriteCollection>(http.delete(`/collections/${collectionId}/posts/${postId}`));
}
