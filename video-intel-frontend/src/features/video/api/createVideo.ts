import type { Owner } from '@/features/user/types/owner';
import { apiFetch } from '@/lib/apiClient';
import type { Video } from '../types/video';

export function createVideo(owner: Owner, title: string): Promise<Video> {
  return apiFetch<Video>('/api/v1/videos', {
    method: 'POST',
    body: { ownerType: owner.ownerType, ownerId: owner.ownerId, title },
  });
}
