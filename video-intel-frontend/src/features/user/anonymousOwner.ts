import { createAnonymousSession } from './api/createAnonymousSession';
import type { Owner } from './types/owner';

const STORAGE_KEY = 'video-intel:anonymous-session';

interface StoredSession {
  id: string;
  expiresAt: string;
}

function readStoredSession(): StoredSession | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return null;
    const parsed = JSON.parse(raw) as StoredSession;
    return new Date(parsed.expiresAt).getTime() > Date.now() ? parsed : null;
  } catch {
    return null;
  }
}

function storeSession(session: StoredSession): void {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
  } catch {
    // best-effort persistence — worst case a fresh session is created next time
  }
}

// TODO: once login/JWT lands, this should prefer an authenticated user's
// owner over the anonymous session.
export async function getOrCreateAnonymousOwner(): Promise<Owner> {
  const existing = readStoredSession();
  if (existing) {
    return { ownerType: 'ANONYMOUS_SESSION', ownerId: existing.id };
  }

  const session = await createAnonymousSession();
  storeSession({ id: session.id, expiresAt: session.expiresAt });
  return { ownerType: 'ANONYMOUS_SESSION', ownerId: session.id };
}
