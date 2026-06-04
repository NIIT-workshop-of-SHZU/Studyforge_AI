import { http, unwrap } from '@/api/http';
import type { CommunityHomepageDesign, UploadedFile, UserHomepage } from '@/types/api';

export interface HomepageDraftPayload {
  templateType: string;
  layoutMode: string;
  themeConfig: string;
  customCodeDraft: string;
  mediaLayoutDraft: string;
}

export interface PublishHomepagePayload {
  title: string;
  summary: string;
}

export function getMyHomepage() {
  return unwrap<UserHomepage>(http.get('/users/me/homepage'));
}

export function getUserHomepage(userId: number | string) {
  return unwrap<UserHomepage>(http.get(`/users/${userId}/homepage`));
}

export function saveHomepageDraft(payload: HomepageDraftPayload) {
  return unwrap<UserHomepage>(http.put('/users/me/homepage/draft', payload));
}

export function submitHomepageReview() {
  return unwrap<UserHomepage>(http.post('/users/me/homepage/submit-review'));
}

export function publishHomepageCommunity(payload: PublishHomepagePayload) {
  return unwrap<CommunityHomepageDesign>(http.post('/users/me/homepage/publish-community', payload));
}

export function listCommunityHomepages(keyword = '', limit = 20) {
  return unwrap<CommunityHomepageDesign[]>(
    http.get('/community/homepages', {
      params: { keyword, limit }
    })
  );
}

export function getCommunityHomepage(designId: number | string) {
  return unwrap<CommunityHomepageDesign>(http.get(`/community/homepages/${designId}`));
}

export function cloneCommunityHomepage(designId: number | string) {
  return unwrap<UserHomepage>(http.post(`/community/homepages/${designId}/clone`));
}

export function uploadHomepageMedia(file: File) {
  const body = new FormData();
  body.append('file', file);
  return unwrap<UploadedFile>(http.post('/uploads/homepage-media', body));
}
