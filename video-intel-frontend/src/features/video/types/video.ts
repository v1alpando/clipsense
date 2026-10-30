import type { Owner } from '@/features/user/types/owner';

export type VideoStatus = 'AWAITING_UPLOAD' | 'UPLOADING' | 'UPLOADED' | 'FAILED';

export interface Video extends Owner {
  id: string;
  title: string;
  status: VideoStatus;
  storageRef: string | null;
  createdAt: string;
  updatedAt: string;
  failedReason: string | null;
}

export function isTerminalVideoStatus(status: VideoStatus): boolean {
  return status === 'UPLOADED' || status === 'FAILED';
}
