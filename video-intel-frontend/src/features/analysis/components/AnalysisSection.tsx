import { Loader2, XCircle } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { useAnalysis } from '../hooks/useAnalysis';

interface AnalysisSectionProps {
  videoId: string;
  enabled: boolean;
}

const JOB_STATUS_LABEL = {
  QUEUED: 'Queued for analysis...',
  RUNNING: 'Analyzing video...',
} as const;

export function AnalysisSection({ videoId, enabled }: AnalysisSectionProps) {
  const { state } = useAnalysis(videoId, enabled);

  return (
    <Card>
      <CardHeader>
        <CardTitle>Analysis</CardTitle>
      </CardHeader>
      <CardContent className="text-sm text-muted-foreground">
        {!enabled && 'Analysis will start once the video finishes uploading.'}

        {enabled && (state.status === 'idle' || state.status === 'loading') && (
          <span className="flex items-center gap-2">
            <Loader2 className="size-4 animate-spin" />
            Starting analysis...
          </span>
        )}

        {enabled && state.status === 'error' && (
          <span className="flex items-center gap-2 text-destructive">
            <XCircle className="size-4 shrink-0" />
            {state.message}
          </span>
        )}

        {enabled && state.status === 'ready' && state.job.status === 'FAILED' && (
          <span className="flex items-center gap-2 text-destructive">
            <XCircle className="size-4 shrink-0" />
            {state.job.failureReason ?? 'Analysis failed.'}
          </span>
        )}

        {enabled && state.status === 'ready' && state.job.status === 'COMPLETED' && (
          // No backend endpoint exposes transcript/summary/key-moments/etc.
          // yet — real sections replace this once that content exists.
          <span>Analysis complete. Detailed results aren&apos;t available from the backend yet.</span>
        )}

        {enabled &&
          state.status === 'ready' &&
          (state.job.status === 'QUEUED' || state.job.status === 'RUNNING') && (
            <span className="flex items-center gap-2">
              <Loader2 className="size-4 animate-spin" />
              {JOB_STATUS_LABEL[state.job.status]}
            </span>
          )}
      </CardContent>
    </Card>
  );
}
