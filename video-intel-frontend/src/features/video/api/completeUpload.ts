import { apiFetch } from '@/lib/apiClient';
import type { Video } from '../types/video';

export function completeUpload(videoId: string): Promise<Video> {
  return apiFetch<Video>(`/api/v1/videos/${videoId}/complete`, { method: 'POST' });
}
