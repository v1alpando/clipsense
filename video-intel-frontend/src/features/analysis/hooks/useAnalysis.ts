import { useCallback, useEffect, useState } from 'react';
import { getAnalysisJob } from '../api/getAnalysisJob';
import { requestAnalysis } from '../api/requestAnalysis';
import { isTerminalAnalysisStatus, type AnalysisJob } from '../types/analysisJob';

type AnalysisState =
  | { status: 'idle' }
  | { status: 'loading' }
  | { status: 'ready'; job: AnalysisJob }
  | { status: 'error'; message: string };

const POLL_INTERVAL_MS = 3000;

// Auto-starts analysis once `enabled` (the video is UPLOADED), then polls
// the job until it reaches a terminal status. Mirrors useVideo's polling
// shape for consistency.
export function useAnalysis(videoId: string, enabled: boolean) {
  const [state, setState] = useState<AnalysisState>({ status: 'idle' });

  const start = useCallback(async () => {
    setState({ status: 'loading' });
    try {
      const job = await requestAnalysis(videoId);
      setState({ status: 'ready', job });
    } catch (error) {
      setState({
        status: 'error',
        message: error instanceof Error ? error.message : 'Failed to start analysis.',
      });
    }
  }, [videoId]);

  useEffect(() => {
    if (!enabled || state.status !== 'idle') return;
    // Standard fetch-on-condition effect — setState inside `start` only runs
    // after its `await`, not synchronously here.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    start();
  }, [enabled, state.status, start]);

  const refetch = useCallback(async () => {
    if (state.status !== 'ready') return;
    try {
      const job = await getAnalysisJob(state.job.id);
      setState({ status: 'ready', job });
    } catch (error) {
      setState({
        status: 'error',
        message: error instanceof Error ? error.message : 'Failed to load analysis status.',
      });
    }
  }, [state]);

  useEffect(() => {
    if (state.status !== 'ready' || isTerminalAnalysisStatus(state.job.status)) return;
    const timeoutId = setTimeout(refetch, POLL_INTERVAL_MS);
    return () => clearTimeout(timeoutId);
  }, [state, refetch]);

  return { state };
}
