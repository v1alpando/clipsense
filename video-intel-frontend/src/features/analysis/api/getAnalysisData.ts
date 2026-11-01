import { apiFetch } from '@/lib/apiClient';
import type { AnalysisData } from '../types/analysisData';

export function getAnalysisData(videoId: string): Promise<AnalysisData> {
  return apiFetch<AnalysisData>(`/api/v1/videos/${videoId}/insights`);
}
