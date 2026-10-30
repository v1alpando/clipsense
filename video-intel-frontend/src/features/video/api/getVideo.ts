import { apiFetch } from '@/lib/apiClient';
import type { Video } from '../types/video';

export function getVideo(videoId: string): Promise<Video> {
  return apiFetch<Video>(`/api/v1/videos/${videoId}`);
}
