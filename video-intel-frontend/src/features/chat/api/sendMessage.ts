import { apiFetch } from '@/lib/apiClient';
import type { ChatMessage } from '../types/chatMessage';

// Returns ONLY the assistant's reply — the backend persists the user's own
// message but doesn't echo it back, so callers must render it themselves.
export function sendMessage(sessionId: string, content: string): Promise<ChatMessage> {
  return apiFetch<ChatMessage>(`/api/v1/chat-sessions/${sessionId}/messages`, {
    method: 'POST',
    body: { content },
  });
}
