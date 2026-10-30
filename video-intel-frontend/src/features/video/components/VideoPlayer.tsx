import { Film } from 'lucide-react';

interface VideoPlayerProps {
  src: string | null;
}

export function VideoPlayer({ src }: VideoPlayerProps) {
  if (!src) {
    return (
      <div className="flex aspect-video w-full flex-col items-center justify-center gap-2 rounded-lg border bg-muted text-sm text-muted-foreground">
        <Film className="size-8" />
        <p>Preview unavailable</p>
      </div>
    );
  }

  return (
    <video controls className="aspect-video w-full rounded-lg border bg-black" src={src} />
  );
}
