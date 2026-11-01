import { useMemo } from 'react';
import { Bookmark, ListVideo, Volume2 } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { formatDuration, formatTimestamp } from '@/lib/formatTime';
import type { KeyMoment } from '../types/analysisData';

interface KeyMomentsListProps {
  moments: KeyMoment[];
  currentTime?: number;
  onSeek?: (seconds: number) => void;
  className?: string;
}

export function KeyMomentsList({
  moments,
  currentTime = 0,
  onSeek,
  className = '',
}: KeyMomentsListProps) {
  // Sort moments ascending by timestamp just in case
  const sortedMoments = useMemo(() => {
    return [...moments].sort((a, b) => a.timestamp - b.timestamp);
  }, [moments]);

  // Determine active chapter index
  const activeIndex = useMemo(() => {
    if (currentTime < 0 || sortedMoments.length === 0) return -1;

    for (let i = 0; i < sortedMoments.length; i++) {
      const current = sortedMoments[i];
      const next = sortedMoments[i + 1];
      if (currentTime >= current.timestamp && (!next || currentTime < next.timestamp)) {
        return i;
      }
    }
    return -1;
  }, [currentTime, sortedMoments]);

  if (sortedMoments.length === 0) {
    return null;
  }

  return (
    <Card className={`overflow-hidden border shadow-sm ${className}`}>
      <CardHeader className="flex flex-row items-center justify-between pb-3">
        <div className="flex items-center gap-2">
          <div className="flex size-7 items-center justify-center rounded-md bg-primary/10 text-primary">
            <ListVideo className="size-4" />
          </div>
          <CardTitle className="text-base font-semibold">
            Video Chapters & Key Moments
          </CardTitle>
        </div>

        <span className="text-xs text-muted-foreground font-mono">
          {sortedMoments.length} chapters
        </span>
      </CardHeader>

      <CardContent className="p-0">
        <div className="divide-y divide-border/60">
          {sortedMoments.map((moment, index) => {
            const isActive = index === activeIndex;
            const nextMoment = sortedMoments[index + 1];
            const chapterDuration =
              moment.duration ||
              (nextMoment ? nextMoment.timestamp - moment.timestamp : undefined);

            return (
              <div
                key={moment.id || `km-${moment.timestamp}-${index}`}
                onClick={() => onSeek?.(moment.timestamp)}
                className={`group flex cursor-pointer items-start gap-3 p-3 transition-colors ${
                  isActive
                    ? 'border-l-4 border-l-primary bg-primary/5 font-medium text-foreground'
                    : 'border-l-4 border-l-transparent hover:bg-muted/50 text-foreground/80'
                }`}
              >
                {/* Timestamp Button */}
                <button
                  type="button"
                  onClick={(e) => {
                    e.stopPropagation();
                    onSeek?.(moment.timestamp);
                  }}
                  className={`inline-flex shrink-0 items-center gap-1.5 rounded px-2 py-1 font-mono text-xs transition-all ${
                    isActive
                      ? 'bg-primary text-primary-foreground font-semibold shadow-xs'
                      : 'bg-muted text-muted-foreground group-hover:bg-primary/10 group-hover:text-primary'
                  }`}
                  title={`Jump video to ${formatTimestamp(moment.timestamp)}`}
                  aria-label={`Jump to chapter ${moment.title} at ${formatTimestamp(moment.timestamp)}`}
                >
                  {isActive ? (
                    <Volume2 className="size-3.5 animate-pulse text-primary-foreground" />
                  ) : (
                    <Bookmark className="size-3.5 opacity-60" />
                  )}
                  {formatTimestamp(moment.timestamp, { brackets: true })}
                </button>

                {/* Chapter Content */}
                <div className="flex-1 min-w-0">
                  <div className="flex items-center justify-between gap-2">
                    <h4
                      className={`truncate text-sm font-medium ${
                        isActive ? 'text-primary' : 'text-foreground'
                      }`}
                    >
                      {moment.title}
                    </h4>

                    {chapterDuration !== undefined && chapterDuration > 0 && (
                      <span className="shrink-0 font-mono text-[11px] text-muted-foreground">
                        {formatDuration(chapterDuration)}
                      </span>
                    )}
                  </div>

                  {moment.description && (
                    <p className="mt-0.5 line-clamp-2 text-xs text-muted-foreground leading-relaxed">
                      {moment.description}
                    </p>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      </CardContent>
    </Card>
  );
}
