export type MessageRole = 'USER' | 'ASSISTANT';

export interface ChatMessage {
  id: string;
  sessionId: string;
  role: MessageRole;
  content: string;
  createdAt: string;
}
