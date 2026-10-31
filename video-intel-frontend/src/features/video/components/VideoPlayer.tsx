import { Film } from 'lucide-react';
import type React from 'react';

export interface VideoPlayerProps {
  src: string | null;
  videoRef?: React.Ref<HTMLVideoElement>;
  onTimeUpdate?: (currentTime: number) => void;
  className?: string;
}

export function VideoPlayer({
  src,
  videoRef,
  onTimeUpdate,
  className = '',
}: VideoPlayerProps) {
  if (!src) {
    return (
      <div
        className={`flex aspect-video w-full flex-col items-center justify-center gap-2 rounded-lg border bg-muted text-sm text-muted-foreground ${className}`}
      >
        <Film className="size-8" />
        <p>Preview unavailable</p>
      </div>
    );
  }

  return (
    <video
      ref={videoRef}
      controls
      className={`aspect-video w-full rounded-lg border bg-black ${className}`}
      src={src}
      onTimeUpdate={(e) => onTimeUpdate?.(e.currentTarget.currentTime)}
    />
  );
}
