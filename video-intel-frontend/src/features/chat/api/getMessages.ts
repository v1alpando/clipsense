import { apiFetch } from '@/lib/apiClient';
import type { ChatMessage } from '../types/chatMessage';

export function getMessages(sessionId: string): Promise<ChatMessage[]> {
  return apiFetch<ChatMessage[]>(`/api/v1/chat-sessions/${sessionId}/messages`);
}
