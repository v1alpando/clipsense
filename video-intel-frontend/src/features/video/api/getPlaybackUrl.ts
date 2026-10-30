import { apiFetch } from '@/lib/apiClient';

export function getPlaybackUrl(videoId: string): Promise<{ playbackUrl: string }> {
  return apiFetch(`/api/v1/videos/${videoId}/playback-url`);
}
