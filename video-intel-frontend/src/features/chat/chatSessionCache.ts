import type { Owner } from '@/features/user/types/owner';
import { createChatSession } from './api/createChatSession';

const STORAGE_KEY_PREFIX = 'video-intel:chat-session:';

function readCachedSessionId(videoId: string): string | null {
  try {
    return localStorage.getItem(STORAGE_KEY_PREFIX + videoId);
  } catch {
    return null;
  }
}

function cacheSessionId(videoId: string, sessionId: string): void {
  try {
    localStorage.setItem(STORAGE_KEY_PREFIX + videoId, sessionId);
  } catch {
    // best-effort — worst case a new session gets created next time
  }
}

// Unlike analysis's request endpoint, chat-session creation isn't documented
// as idempotent — there's also no list-sessions-for-video endpoint — so the
// session id is cached client-side once created, one per video.
export async function getOrCreateChatSessionId(videoId: string, owner: Owner): Promise<string> {
  const cached = readCachedSessionId(videoId);
  if (cached) return cached;

  const session = await createChatSession(videoId, owner);
  cacheSessionId(videoId, session.id);
  return session.id;
}
