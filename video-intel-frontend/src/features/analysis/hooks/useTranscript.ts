import { useCallback, useEffect, useState } from 'react';
import { getTranscript } from '../api/getTranscript';
import type { TranscriptResponse } from '../types/transcript';

export type TranscriptState =
  | { status: 'idle' }
  | { status: 'loading' }
  | { status: 'ready'; transcript: TranscriptResponse }
  | { status: 'error'; message: string };

export function useTranscript(videoId: string, enabled = true) {
  const [state, setState] = useState<TranscriptState>({ status: 'idle' });

  const fetchTranscript = useCallback(async () => {
    setState({ status: 'loading' });
    try {
      const data = await getTranscript(videoId);
      setState({ status: 'ready', transcript: data });
    } catch (error) {
      setState({
        status: 'error',
        message:
          error instanceof Error
            ? error.message
            : 'Failed to load video transcript.',
      });
    }
  }, [videoId]);

  useEffect(() => {
    if (!enabled) return;
    // eslint-disable-next-line react-hooks/set-state-in-effect
    fetchTranscript();
  }, [enabled, fetchTranscript]);

  return { state, refetch: fetchTranscript };
}
