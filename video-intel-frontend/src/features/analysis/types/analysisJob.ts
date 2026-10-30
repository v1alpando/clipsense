export type AnalysisStatus = 'QUEUED' | 'RUNNING' | 'COMPLETED' | 'FAILED';

export interface AnalysisJob {
  id: string;
  videoId: string;
  status: AnalysisStatus;
  failureReason: string | null;
  createdAt: string;
  updatedAt: string;
}

export function isTerminalAnalysisStatus(status: AnalysisStatus): boolean {
  return status === 'COMPLETED' || status === 'FAILED';
}
