import { useCallback, useEffect, useState } from 'react';
import { getPlaybackUrl } from '../api/getPlaybackUrl';

type PlaybackUrlState =
  | { status: 'idle' }
  | { status: 'loading' }
  | { status: 'ready'; url: string }
  | { status: 'error' };

export function usePlaybackUrl(videoId: string, enabled: boolean) {
  const [state, setState] = useState<PlaybackUrlState>({ status: 'idle' });

  const fetchUrl = useCallback(async () => {
    setState({ status: 'loading' });
    try {
      const { playbackUrl } = await getPlaybackUrl(videoId);
      setState({ status: 'ready', url: playbackUrl });
    } catch {
      setState({ status: 'error' });
    }
  }, [videoId]);

  useEffect(() => {
    if (!enabled) return;
    // eslint-disable-next-line react-hooks/set-state-in-effect
    fetchUrl();
  }, [enabled, fetchUrl]);

  return state;
}
