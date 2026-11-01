import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  Check,
  Clock,
  Copy,
  FileText,
  Loader2,
  MessageSquare,
  RefreshCw,
  Search,
  Volume2,
  X,
  XCircle,
} from 'lucide-react';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { formatDuration, formatTimestamp } from '@/lib/formatTime';
import { useTranscript } from '../hooks/useTranscript';
import type { TranscriptResponse, TranscriptSegment } from '../types/transcript';

interface TranscriptViewerProps {
  /** If provided, loads transcript via useTranscript hook. */
  videoId?: string;
  /** Or directly pass the transcript response. */
  transcript?: TranscriptResponse;
  /** Or directly pass an array of segments. */
  segments?: TranscriptSegment[];
  /** Current playback time in seconds from the VideoPlayer. */
  currentTime?: number;
  /** Callback fired when a segment or timestamp is clicked. */
  onSeek?: (seconds: number) => void;
  className?: string;
}

export function TranscriptViewer({
  videoId,
  transcript: directTranscript,
  segments: directSegments,
  currentTime = 0,
  onSeek,
  className = '',
}: TranscriptViewerProps) {
  // If videoId is provided and no direct transcript/segments were passed, use hook with auto-retry
  const { state: hookState, refetch } = useTranscript(
    videoId ?? '',
    Boolean(videoId && !directTranscript && !directSegments),
  );

  const [searchQuery, setSearchQuery] = useState('');
  const [autoScroll, setAutoScroll] = useState(true);
  const [copied, setCopied] = useState(false);
  const activeSegmentRef = useRef<HTMLDivElement | null>(null);
  const scrollContainerRef = useRef<HTMLDivElement | null>(null);

  const resolvedTranscript =
    directTranscript ?? (hookState.status === 'ready' ? hookState.transcript : null);

  const segments = useMemo(() => {
    return directSegments ?? resolvedTranscript?.segments ?? [];
  }, [directSegments, resolvedTranscript]);

  // Aggregate stats: total speech duration and total phrases
  const totalSpeechDuration = useMemo(() => {
    return segments.reduce(
      (acc, seg) => acc + Math.max(0, seg.end - seg.start),
      0,
    );
  }, [segments]);

  // Determine active segment index based on current playback time
  const activeIndex = useMemo(() => {
    if (currentTime <= 0 || segments.length === 0) return -1;
    return segments.findIndex(
      (segment) => currentTime >= segment.start && currentTime < segment.end,
    );
  }, [currentTime, segments]);

  // Filter segments by search query
  const filteredSegments = useMemo(() => {
    if (!searchQuery.trim()) return segments;
    const lower = searchQuery.toLowerCase();
    return segments.filter((segment) => segment.text.toLowerCase().includes(lower));
  }, [segments, searchQuery]);

  // Auto-scroll to active segment while video is playing
  useEffect(() => {
    if (!autoScroll || activeIndex === -1 || searchQuery.trim() !== '') return;
    if (activeSegmentRef.current && scrollContainerRef.current) {
      activeSegmentRef.current.scrollIntoView({
        behavior: 'smooth',
        block: 'nearest',
      });
    }
  }, [activeIndex, autoScroll, searchQuery]);

  const handleCopy = useCallback(async () => {
    if (!resolvedTranscript && segments.length === 0) return;
    try {
      const fullText =
        resolvedTranscript?.text ||
        segments
          .map(
            (seg) =>
              `${formatTimestamp(seg.start, { brackets: true })} ${seg.text}`,
          )
          .join('\n');

      await navigator.clipboard.writeText(fullText);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      // Clipboard write failed (permissions/unfocused)
    }
  }, [resolvedTranscript, segments]);

  const handleSegmentClick = useCallback(
    (startSeconds: number) => {
      onSeek?.(startSeconds);
    },
    [onSeek],
  );

  // If loading from hook
  if (!directTranscript && !directSegments && hookState.status === 'loading') {
    return (
      <div className="flex flex-col items-center justify-center gap-3 py-12 text-center text-sm text-muted-foreground">
        <Loader2 className="size-6 animate-spin text-primary" />
        <p>Loading transcript...</p>
      </div>
    );
  }

  // If error from hook
  if (!directTranscript && !directSegments && hookState.status === 'error') {
    return (
      <div className="flex flex-col items-center gap-3 rounded-lg border border-destructive/40 bg-destructive/5 p-6 text-center text-sm">
        <div className="flex items-center gap-2 text-destructive font-medium">
          <XCircle className="size-5 shrink-0" />
          <span>Could not load transcript</span>
        </div>
        <p className="max-w-md text-muted-foreground">{hookState.message}</p>
        <Button variant="outline" size="sm" onClick={() => refetch()} className="mt-2 gap-1.5">
          <RefreshCw className="size-3.5" />
          Try Again
        </Button>
      </div>
    );
  }

  if (segments.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center gap-2 rounded-lg border border-dashed py-10 text-center text-sm text-muted-foreground">
        <FileText className="size-7 opacity-40" />
        <p>No transcript segments available for this video.</p>
      </div>
    );
  }

  return (
    <div className={`flex flex-col gap-3 ${className}`}>
      {/* Header controls: Search & Action buttons */}
      <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
        <div className="relative flex-1">
          <Search className="absolute left-2.5 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
          <Input
            type="text"
            placeholder="Search transcript..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="pl-8 pr-8 text-sm"
          />
          {searchQuery && (
            <button
              type="button"
              onClick={() => setSearchQuery('')}
              className="absolute right-2.5 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground"
              aria-label="Clear search"
            >
              <X className="size-3.5" />
            </button>
          )}
        </div>

        <div className="flex items-center gap-2">
          <Button
            type="button"
            variant="ghost"
            size="sm"
            onClick={() => setAutoScroll((prev) => !prev)}
            className={`text-xs ${autoScroll ? 'text-primary font-medium' : 'text-muted-foreground'}`}
            title="Auto-scroll transcript to currently playing video timestamp"
          >
            Auto-scroll: {autoScroll ? 'On' : 'Off'}
          </Button>

          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={handleCopy}
            className="gap-1.5 text-xs"
          >
            {copied ? (
              <>
                <Check className="size-3.5 text-green-600" />
                Copied
              </>
            ) : (
              <>
                <Copy className="size-3.5" />
                Copy
              </>
            )}
          </Button>
        </div>
      </div>

      {/* Meta Stats bar: total phrases, filtered phrases, speech duration */}
      <div className="flex flex-wrap items-center justify-between gap-2 border-b pb-2 text-xs text-muted-foreground">
        <div className="flex items-center gap-2">
          <span className="inline-flex items-center gap-1 font-medium text-foreground">
            <MessageSquare className="size-3.5 text-muted-foreground" />
            {searchQuery.trim() ? (
              <span>
                Found <span className="font-semibold text-primary">{filteredSegments.length}</span> of {segments.length} phrases
              </span>
            ) : (
              <span>{segments.length} phrases</span>
            )}
          </span>
          {searchQuery.trim() && (
            <Badge variant="secondary" className="h-5 px-1.5 text-[11px] font-normal">
              Filtered
            </Badge>
          )}
        </div>

        {totalSpeechDuration > 0 && (
          <div className="inline-flex items-center gap-1 text-muted-foreground">
            <Clock className="size-3.5" />
            <span>Speech: {formatDuration(totalSpeechDuration)}</span>
          </div>
        )}
      </div>

      {/* Segments scrollable list */}
      <div
        ref={scrollContainerRef}
        className="flex max-h-[440px] flex-col divide-y divide-border/50 overflow-y-auto rounded-lg border bg-card text-card-foreground shadow-sm"
      >
        {filteredSegments.length === 0 ? (
          <div className="py-8 text-center text-sm text-muted-foreground">
            No phrases found matching &ldquo;{searchQuery}&rdquo;
          </div>
        ) : (
          filteredSegments.map((segment) => {
            const isPlaying =
              currentTime >= segment.start && currentTime < segment.end;
            const originalIndex = segments.indexOf(segment);
            const isCurrentActive = originalIndex === activeIndex;
            const segmentDuration = Math.max(0, segment.end - segment.start);

            return (
              <div
                key={`${segment.start}-${segment.end}`}
                ref={isCurrentActive ? activeSegmentRef : null}
                onClick={() => handleSegmentClick(segment.start)}
                className={`group flex cursor-pointer items-start gap-3 p-3 text-sm transition-colors ${
                  isPlaying
                    ? 'border-l-4 border-l-primary bg-primary/5 font-medium text-foreground'
                    : 'border-l-4 border-l-transparent hover:bg-muted/50'
                }`}
              >
                {/* Timestamp Button & Duration Badge */}
                <div className="flex shrink-0 items-center gap-1.5 pt-0.5">
                  <button
                    type="button"
                    onClick={(e) => {
                      e.stopPropagation();
                      handleSegmentClick(segment.start);
                    }}
                    className={`inline-flex items-center gap-1 rounded px-1.5 py-0.5 font-mono text-xs transition-colors ${
                      isPlaying
                        ? 'bg-primary text-primary-foreground font-semibold'
                        : 'bg-muted text-muted-foreground group-hover:bg-primary/10 group-hover:text-primary'
                    }`}
                    title={`Jump video to ${formatTimestamp(segment.start)}`}
                    aria-label={`Jump to ${formatTimestamp(segment.start)}`}
                  >
                    {isPlaying && <Volume2 className="size-3 animate-pulse" />}
                    {formatTimestamp(segment.start, { brackets: true })}
                  </button>

                  {/* Segment Duration Badge */}
                  <span
                    className="font-mono text-[11px] text-muted-foreground/70"
                    title={`Duration: ${segmentDuration.toFixed(2)}s`}
                  >
                    {formatDuration(segmentDuration)}
                  </span>
                </div>

                {/* Segment Text with search highlighting */}
                <div className="flex-1 leading-relaxed">
                  <HighlightedText text={segment.text} query={searchQuery} />
                </div>
              </div>
            );
          })
        )}
      </div>
    </div>
  );
}

/**
 * Safely highlights occurrences of search query inside the segment text.
 */
function HighlightedText({ text, query }: { text: string; query: string }) {
  if (!query.trim()) {
    return <span>{text}</span>;
  }

  const escaped = query.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  const regex = new RegExp(`(${escaped})`, 'gi');
  const parts = text.split(regex);

  return (
    <span>
      {parts.map((part, index) =>
        part.toLowerCase() === query.toLowerCase() ? (
          <mark
            key={index}
            className="rounded bg-yellow-200 px-0.5 font-medium text-foreground dark:bg-yellow-800/80 dark:text-yellow-100"
          >
            {part}
          </mark>
        ) : (
          part
        ),
      )}
    </span>
  );
}
