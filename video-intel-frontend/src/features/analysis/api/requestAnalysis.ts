import { apiFetch } from '@/lib/apiClient';
import type { AnalysisJob } from '../types/analysisJob';

// Idempotent on the backend: one job per video ever exists, so calling this
// again for the same video just returns the existing job — also how a client
// recovers the job id if it didn't persist one (there's no list-by-video
// endpoint).
export function requestAnalysis(videoId: string): Promise<AnalysisJob> {
  return apiFetch<AnalysisJob>(`/api/v1/videos/${videoId}/analysis`, { method: 'POST' });
}
