import { apiFetch } from '@/lib/apiClient';

export interface AnonymousSession {
  id: string;
  createdAt: string;
  expiresAt: string;
}

export function createAnonymousSession(): Promise<AnonymousSession> {
  return apiFetch<AnonymousSession>('/api/v1/anonymous-sessions', {
    method: 'POST',
  });
}
