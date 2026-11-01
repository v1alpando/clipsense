import { FileText, Loader2, Sparkles, XCircle } from 'lucide-react';
import { Badge } from '@/components/ui/badge';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { useAnalysis } from '../hooks/useAnalysis';
import { useAnalysisData } from '../hooks/useAnalysisData';
import { useTranscript } from '../hooks/useTranscript';
import { ExecutiveSummaryCard } from './ExecutiveSummaryCard';
import { HookAnalysisCard } from './HookAnalysisCard';
import { KeyMomentsList } from './KeyMomentsList';
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
  currentTime = 0,
  onSeek,
}: AnalysisSectionProps) {
  const { state: analysisJobState } = useAnalysis(videoId, enabled);

  const isCompleted =
    enabled &&
    analysisJobState.status === 'ready' &&
    analysisJobState.job.status === 'COMPLETED';

  // Load transcript data
  const { state: transcriptState } = useTranscript(videoId, isCompleted);

  // Load derived / native AI insights (Hook, Chapters, Summary)
  const resolvedTranscript =
    transcriptState.status === 'ready' ? transcriptState.transcript : null;
  const { state: insightsState } = useAnalysisData(
    videoId,
    resolvedTranscript,
    isCompleted,
  );

  return (
    <Card className="shadow-sm">
      <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-3">
        <div className="flex items-center gap-2">
          <CardTitle className="text-lg font-semibold tracking-tight">
            AI Video Intelligence
          </CardTitle>
        </div>

        {isCompleted && (
          <Badge variant="secondary" className="gap-1 text-xs">
            <Sparkles className="size-3 text-primary" />
            Analyzed
          </Badge>
        )}
      </CardHeader>

      <CardContent className="text-sm">
        {!enabled && (
          <p className="text-muted-foreground">
            Analysis will start once the video finishes uploading.
          </p>
        )}

        {enabled &&
          (analysisJobState.status === 'idle' ||
            analysisJobState.status === 'loading') && (
            <div className="flex items-center gap-2 text-muted-foreground py-4">
              <Loader2 className="size-4 animate-spin text-primary" />
              <span>Starting video analysis...</span>
            </div>
          )}

        {enabled && analysisJobState.status === 'error' && (
          <div className="flex items-center gap-2 rounded-lg border border-destructive/40 bg-destructive/5 p-3 text-destructive">
            <XCircle className="size-4 shrink-0" />
            <span>{analysisJobState.message}</span>
          </div>
        )}

        {enabled &&
          analysisJobState.status === 'ready' &&
          analysisJobState.job.status === 'FAILED' && (
            <div className="flex items-center gap-2 rounded-lg border border-destructive/40 bg-destructive/5 p-3 text-destructive">
              <XCircle className="size-4 shrink-0" />
              <span>
                {analysisJobState.job.failureReason ?? 'Analysis failed.'}
              </span>
            </div>
          )}

        {enabled &&
          analysisJobState.status === 'ready' &&
          (analysisJobState.job.status === 'QUEUED' ||
            analysisJobState.job.status === 'RUNNING') && (
            <div className="flex items-center gap-2 text-muted-foreground py-4">
              <Loader2 className="size-4 animate-spin text-primary" />
              <span>{JOB_STATUS_LABEL[analysisJobState.job.status]}</span>
            </div>
          )}

        {/* Studio View when Analysis is Completed */}
        {isCompleted && (
          <div className="pt-1">
            <Tabs defaultValue="insights" className="w-full">
              <TabsList className="grid w-full max-w-xs grid-cols-2 mb-4">
                <TabsTrigger value="insights" className="gap-1.5 text-xs">
                  <Sparkles className="size-3.5 text-primary" />
                  AI Insights
                </TabsTrigger>
                <TabsTrigger value="transcript" className="gap-1.5 text-xs">
                  <FileText className="size-3.5" />
                  Transcript
                </TabsTrigger>
              </TabsList>

              {/* Tab 1: AI Insights Dashboard (Hook, Executive Summary, Key Moments) */}
              <TabsContent value="insights" className="flex flex-col gap-4">
                {insightsState.status === 'loading' ? (
                  <div className="flex items-center justify-center gap-2 py-8 text-muted-foreground">
                    <Loader2 className="size-5 animate-spin text-primary" />
                    <span>Extracting video intelligence...</span>
                  </div>
                ) : insightsState.status === 'ready' ? (
                  <>
                    <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
                      <HookAnalysisCard
                        hook={insightsState.data.hook}
                        onSeek={onSeek}
                      />
                      <ExecutiveSummaryCard
                        summary={insightsState.data.summary}
                      />
                    </div>

                    <KeyMomentsList
                      moments={insightsState.data.keyMoments}
                      currentTime={currentTime}
                      onSeek={onSeek}
                    />
                  </>
                ) : null}
              </TabsContent>

              {/* Tab 2: Interactive Transcript Viewer */}
              <TabsContent value="transcript">
                <TranscriptViewer
                  videoId={videoId}
                  transcript={resolvedTranscript ?? undefined}
                  currentTime={currentTime}
                  onSeek={onSeek}
                />
              </TabsContent>
            </Tabs>
          </div>
        )}
      </CardContent>
    </Card>
  );
}
