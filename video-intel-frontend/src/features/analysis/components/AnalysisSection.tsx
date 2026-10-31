import { Loader2, XCircle } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { useAnalysis } from '../hooks/useAnalysis';
import { TranscriptViewer } from './TranscriptViewer';

interface AnalysisSectionProps {
  videoId: string;
  enabled: boolean;
  currentTime?: number;
  onSeek?: (time: number) => void;
}

const JOB_STATUS_LABEL = {
  QUEUED: 'Queued for analysis...',
  RUNNING: 'Analyzing video & generating transcript...',
} as const;

export function AnalysisSection({
  videoId,
  enabled,
  currentTime,
  onSeek,
}: AnalysisSectionProps) {
  const { state } = useAnalysis(videoId, enabled);

  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-3">
        <CardTitle className="text-lg font-semibold">Transcript & Analysis</CardTitle>
        {enabled && state.status === 'ready' && state.job.status === 'COMPLETED' && (
          <Badge variant="secondary" className="text-xs">
            Completed
          </Badge>
        )}
      </CardHeader>
      <CardContent className="text-sm">
        {!enabled && (
          <p className="text-muted-foreground">
            Analysis will start once the video finishes uploading.
          </p>
        )}

        {enabled && (state.status === 'idle' || state.status === 'loading') && (
          <div className="flex items-center gap-2 text-muted-foreground py-4">
            <Loader2 className="size-4 animate-spin text-primary" />
            <span>Starting analysis...</span>
          </div>
        )}

        {enabled && state.status === 'error' && (
          <div className="flex items-center gap-2 rounded-lg border border-destructive/40 bg-destructive/5 p-3 text-destructive">
            <XCircle className="size-4 shrink-0" />
            <span>{state.message}</span>
          </div>
        )}

        {enabled && state.status === 'ready' && state.job.status === 'FAILED' && (
          <div className="flex items-center gap-2 rounded-lg border border-destructive/40 bg-destructive/5 p-3 text-destructive">
            <XCircle className="size-4 shrink-0" />
            <span>{state.job.failureReason ?? 'Analysis failed.'}</span>
          </div>
        )}

        {enabled &&
          state.status === 'ready' &&
          (state.job.status === 'QUEUED' || state.job.status === 'RUNNING') && (
            <div className="flex items-center gap-2 text-muted-foreground py-4">
              <Loader2 className="size-4 animate-spin text-primary" />
              <span>{JOB_STATUS_LABEL[state.job.status]}</span>
            </div>
          )}

        {enabled && state.status === 'ready' && state.job.status === 'COMPLETED' && (
          <div className="pt-1">
            <TranscriptViewer
              videoId={videoId}
              currentTime={currentTime}
              onSeek={onSeek}
            />
          </div>
        )}
      </CardContent>
    </Card>
  );
}
