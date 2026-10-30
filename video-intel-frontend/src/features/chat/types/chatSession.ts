import type { Owner } from '@/features/user/types/owner';

export interface ChatSession extends Owner {
  id: string;
  videoId: string;
  createdAt: string;
}
