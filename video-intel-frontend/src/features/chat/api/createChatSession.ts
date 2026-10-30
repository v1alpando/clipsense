import type { Owner } from '@/features/user/types/owner';
import { apiFetch } from '@/lib/apiClient';
import type { ChatSession } from '../types/chatSession';

export function createChatSession(videoId: string, owner: Owner): Promise<ChatSession> {
  return apiFetch<ChatSession>(`/api/v1/videos/${videoId}/chat-sessions`, {
    method: 'POST',
    body: { ownerType: owner.ownerType, ownerId: owner.ownerId },
  });
}
