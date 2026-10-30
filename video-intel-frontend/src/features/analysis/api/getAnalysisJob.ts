import { apiFetch } from '@/lib/apiClient';
import type { AnalysisJob } from '../types/analysisJob';

export function getAnalysisJob(jobId: string): Promise<AnalysisJob> {
  return apiFetch<AnalysisJob>(`/api/v1/analysis-jobs/${jobId}`);
}
