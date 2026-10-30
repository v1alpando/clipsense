import { apiFetch } from '@/lib/apiClient';

export function getUploadUrl(videoId: string): Promise<{ uploadUrl: string }> {
  return apiFetch(`/api/v1/videos/${videoId}/upload-url`, { method: 'POST' });
}
