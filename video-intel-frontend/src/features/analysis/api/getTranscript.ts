import { apiFetch } from '@/lib/apiClient';
import type { TranscriptResponse } from '../types/transcript';
import type { Owner } from '@/features/user/types/owner';

export function getTranscript(videoId: string, owner?: Owner): Promise<TranscriptResponse> {
  const query = owner
    ? `?ownerId=${encodeURIComponent(owner.ownerId)}&ownerType=${encodeURIComponent(owner.ownerType)}`
    : '';
  return apiFetch<TranscriptResponse>(`/api/v1/videos/${videoId}/transcript${query}`);
}
