import { Loader2, XCircle } from 'lucide-react';
import { useParams } from 'react-router-dom';
import { AnalysisSection } from '@/features/analysis/components/AnalysisSection';
import { ChatSection } from '@/features/chat/components/ChatSection';
import { VideoPlayer } from '@/features/video/components/VideoPlayer';
import { VideoStatusBadge } from '@/features/video/components/VideoStatusBadge';
import { usePlaybackUrl } from '@/features/video/hooks/usePlaybackUrl';
import { useVideo } from '@/features/video/hooks/useVideo';
import { isTerminalVideoStatus } from '@/features/video/types/video';
import { formatDateTime } from '@/lib/formatDateTime';

export function VideoPage() {
  const { videoId } = useParams<{ videoId: string }>();
  const { state } = useVideo(videoId!);
  // Called unconditionally (rules of hooks) — enabled only once we know the
  // video is UPLOADED, computed safely before the loading/error early returns.
  const playbackUrlState = usePlaybackUrl(
    videoId!,
    state.status === 'success' && state.video.status === 'UPLOADED',
  );

  if (state.status === 'loading') {
    return (
      <div className="flex flex-col items-center gap-3 py-16 text-center">
        <Loader2 className="size-8 animate-spin text-muted-foreground" />
        <p className="text-sm text-muted-foreground">Loading video...</p>
      </div>
    );
  }

  if (state.status === 'error') {
    return (
      <div className="mx-auto flex max-w-md items-start gap-2 rounded-lg border border-destructive/50 bg-destructive/5 p-4 text-sm">
        <XCircle className="size-4 shrink-0 text-destructive" />
        <p>{state.message}</p>
      </div>
    );
  }

  const { video } = state;

  return (
    <div className="flex flex-col gap-6">
      <div className="flex items-center justify-between gap-4">
        <h1 className="truncate text-2xl font-semibold tracking-tight">{video.title}</h1>
        <div className="flex items-center gap-2">
          {!isTerminalVideoStatus(video.status) && (
            <Loader2 className="size-4 animate-spin text-muted-foreground" />
          )}
          <VideoStatusBadge status={video.status} />
        </div>
      </div>

      <VideoPlayer src={playbackUrlState.status === 'ready' ? playbackUrlState.url : null} />

      {video.status === 'FAILED' && video.failedReason && (
        <div className="flex items-start gap-2 rounded-lg border border-destructive/50 bg-destructive/5 p-4 text-sm">
          <XCircle className="size-4 shrink-0 text-destructive" />
          <p>{video.failedReason}</p>
        </div>
      )}

      <dl className="grid grid-cols-2 gap-x-4 gap-y-1 text-sm sm:grid-cols-4">
        <dt className="text-muted-foreground">Created</dt>
        <dd>{formatDateTime(video.createdAt)}</dd>
        <dt className="text-muted-foreground">Updated</dt>
        <dd>{formatDateTime(video.updatedAt)}</dd>
      </dl>

      <AnalysisSection videoId={video.id} enabled={video.status === 'UPLOADED'} />
      <ChatSection videoId={video.id} enabled={video.status === 'UPLOADED'} />
    </div>
  );
}
