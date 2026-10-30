import { Badge } from '@/components/ui/badge';
import type { VideoStatus } from '../types/video';

const STATUS_LABEL: Record<VideoStatus, string> = {
  AWAITING_UPLOAD: 'Awaiting upload',
  UPLOADING: 'Uploading',
  UPLOADED: 'Uploaded',
  FAILED: 'Failed',
};

const STATUS_VARIANT: Record<VideoStatus, 'secondary' | 'outline' | 'destructive'> = {
  AWAITING_UPLOAD: 'outline',
  UPLOADING: 'secondary',
  UPLOADED: 'secondary',
  FAILED: 'destructive',
};

export function VideoStatusBadge({ status }: { status: VideoStatus }) {
  return <Badge variant={STATUS_VARIANT[status]}>{STATUS_LABEL[status]}</Badge>;
}
