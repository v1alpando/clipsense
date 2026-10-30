import { useCallback, useEffect, useState } from 'react';
import { getVideo } from '../api/getVideo';
import { isTerminalVideoStatus, type Video } from '../types/video';

type VideoQueryState =
  | { status: 'loading' }
  | { status: 'success'; video: Video }
  | { status: 'error'; message: string };

const POLL_INTERVAL_MS = 3000;

export function useVideo(videoId: string) {
  const [state, setState] = useState<VideoQueryState>({ status: 'loading' });

  // Deliberately doesn't reset to 'loading' here: the initial state already
  // covers first mount, and a background refetch (polling, retry) should
  // update quietly rather than flashing the spinner over live data.
  const refetch = useCallback(async () => {
    try {
      const video = await getVideo(videoId);
      setState({ status: 'success', video });
    } catch (error) {
      setState({
        status: 'error',
        message: error instanceof Error ? error.message : 'Failed to load video.',
      });
    }
  }, [videoId]);

  useEffect(() => {
    // Standard fetch-on-mount: the setState calls inside refetch only run
    // after its `await`, not synchronously here — this lint rule's static
    // analysis can't see across that boundary.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    refetch();
  }, [refetch]);

  // Poll only while the status is still in flux (e.g. a stale UPLOADING the
  // backend hasn't retried yet). Re-schedules from each result rather than a
  // fixed interval, so overlapping requests can't pile up during a slow one.
  useEffect(() => {
    if (state.status !== 'success' || isTerminalVideoStatus(state.video.status)) {
      return;
    }
    const timeoutId = setTimeout(refetch, POLL_INTERVAL_MS);
    return () => clearTimeout(timeoutId);
  }, [state, refetch]);

  return { state, refetch };
}
