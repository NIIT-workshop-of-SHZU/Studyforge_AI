import type { LoginSession } from '@/types/api';

export const AUTH_STORAGE_KEY = 'studyforge.portal.auth';

function notifyStorageChanged() {
  if (typeof window === 'undefined') {
    return;
  }

  window.dispatchEvent(new CustomEvent('studyforge:portal-auth-changed'));
}

export function readStoredSession(): LoginSession | null {
  if (typeof window === 'undefined') {
    return null;
  }

  const raw = window.localStorage.getItem(AUTH_STORAGE_KEY);

  if (!raw) {
    return null;
  }

  try {
    return JSON.parse(raw) as LoginSession;
  } catch {
    window.localStorage.removeItem(AUTH_STORAGE_KEY);
    return null;
  }
}

export function writeStoredSession(session: LoginSession) {
  window.localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(session));
  notifyStorageChanged();
}

export function clearStoredSession() {
  window.localStorage.removeItem(AUTH_STORAGE_KEY);
  notifyStorageChanged();
}
