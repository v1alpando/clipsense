import { Flame, Lightbulb, Play, Sparkles } from 'lucide-react';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import type { HookAnalysis } from '../types/analysisData';

interface HookAnalysisCardProps {
  hook: HookAnalysis;
  onSeek?: (seconds: number) => void;
  className?: string;
}

export function HookAnalysisCard({
  hook,
  onSeek,
  className = '',
}: HookAnalysisCardProps) {
  const { score, hookType, critique, suggestion } = hook;

  // Color scheme and label depending on score
  const getScoreVariant = (val: number) => {
    if (val >= 8) {
      return {
        badgeClass:
          'bg-emerald-500/15 text-emerald-600 dark:text-emerald-400 border-emerald-500/30 font-semibold',
        label: 'High Retention',
      };
    }
    if (val >= 5) {
      return {
        badgeClass:
          'bg-amber-500/15 text-amber-600 dark:text-amber-400 border-amber-500/30 font-semibold',
        label: 'Average Hook',
      };
    }
    return {
      badgeClass:
        'bg-rose-500/15 text-rose-600 dark:text-rose-400 border-rose-500/30 font-semibold',
      label: 'Needs Improvement',
    };
  };

  const { badgeClass, label } = getScoreVariant(score);

  return (
    <Card className={`overflow-hidden border shadow-sm ${className}`}>
      <CardHeader className="flex flex-row items-center justify-between pb-3">
        <div className="flex items-center gap-2">
          <div className="flex size-7 items-center justify-center rounded-md bg-primary/10 text-primary">
            <Flame className="size-4" />
          </div>
          <CardTitle className="text-base font-semibold">
            Hook & Retention Analysis
          </CardTitle>
        </div>

        {/* Score Badge */}
        <div className="flex items-center gap-2">
          <span className="text-xs text-muted-foreground hidden sm:inline">
            {label}
          </span>
          <Badge variant="outline" className={`text-xs px-2 py-0.5 ${badgeClass}`}>
            {score.toFixed(1)} / 10
          </Badge>
        </div>
      </CardHeader>

      <CardContent className="flex flex-col gap-3 text-sm">
        {/* Hook Type and Replay Button */}
        <div className="flex flex-wrap items-center justify-between gap-2 rounded-lg bg-muted/50 p-2.5">
          <div className="flex items-center gap-1.5 text-xs text-muted-foreground">
            <Sparkles className="size-3.5 text-primary" />
            <span className="font-medium text-foreground">Hook Type:</span>
            <span>{hookType}</span>
          </div>

          <Button
            type="button"
            variant="ghost"
            size="xs"
            onClick={() => onSeek?.(0)}
            className="gap-1 text-xs text-primary hover:text-primary"
            title="Rewind video to the very beginning (00:00) to review the hook"
          >
            <Play className="size-3" />
            Replay Hook (00:00)
          </Button>
        </div>

        {/* Critique section */}
        <div className="space-y-1">
          <h4 className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
            Critique (First 3-5 seconds)
          </h4>
          <p className="leading-relaxed text-foreground/90">{critique}</p>
        </div>

        {/* Actionable Suggestion */}
        <div className="flex items-start gap-2.5 rounded-lg border border-primary/20 bg-primary/5 p-3 text-xs leading-relaxed">
          <Lightbulb className="size-4 shrink-0 text-primary mt-0.5" />
          <div>
            <span className="font-semibold text-primary block mb-0.5">
              Actionable Suggestion:
            </span>
            <span className="text-foreground/90">{suggestion}</span>
          </div>
        </div>
      </CardContent>
    </Card>
  );
}
