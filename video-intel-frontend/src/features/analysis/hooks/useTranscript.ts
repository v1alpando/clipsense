import { useCallback, useEffect, useRef, useState } from 'react';
import { getTranscript } from '../api/getTranscript';
import type { TranscriptResponse } from '../types/transcript';

export type TranscriptState =
  | { status: 'idle' }
  | { status: 'loading' }
  | { status: 'ready'; transcript: TranscriptResponse }
  | { status: 'error'; message: string };

const MAX_AUTO_RETRIES = 3;
const RETRY_DELAY_MS = 1500;

export function useTranscript(videoId: string, enabled = true) {
  const [state, setState] = useState<TranscriptState>({ status: 'idle' });
  const isCancelledRef = useRef(false);

  const fetchTranscript = useCallback(async () => {
    setState((prev) => (prev.status === 'ready' ? prev : { status: 'loading' }));

    for (let attempt = 0; attempt <= MAX_AUTO_RETRIES; attempt++) {
      try {
        const data = await getTranscript(videoId);
        if (!isCancelledRef.current) {
          setState({ status: 'ready', transcript: data });
        }
        return;
      } catch (error) {
        if (isCancelledRef.current) return;

        if (attempt < MAX_AUTO_RETRIES) {
          await new Promise((resolve) => setTimeout(resolve, RETRY_DELAY_MS));
          if (isCancelledRef.current) return;
        } else {
          setState({
            status: 'error',
            message:
              error instanceof Error
                ? error.message
                : 'Failed to load video transcript.',
          });
        }
      }
    }
  }, [videoId]);

  useEffect(() => {
    if (!enabled) return;

    isCancelledRef.current = false;
    // eslint-disable-next-line react-hooks/set-state-in-effect
    fetchTranscript();

    return () => {
      isCancelledRef.current = true;
    };
  }, [enabled, fetchTranscript]);

  return { state, refetch: fetchTranscript };
}
