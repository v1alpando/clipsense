import { useState, useCallback } from 'react';
import { Check, CheckCircle2, Copy, FileText } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import type { ExecutiveSummary } from '../types/analysisData';

interface ExecutiveSummaryCardProps {
  summary: ExecutiveSummary;
  className?: string;
}

export function ExecutiveSummaryCard({
  summary,
  className = '',
}: ExecutiveSummaryCardProps) {
  const [copied, setCopied] = useState(false);
  const { tldr, takeaways } = summary;

  const handleCopy = useCallback(async () => {
    try {
      const textToCopy = `TL;DR:\n${tldr}\n\nKey Takeaways:\n${takeaways
        .map((t, idx) => `${idx + 1}. ${t}`)
        .join('\n')}`;

      await navigator.clipboard.writeText(textToCopy);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      // Clipboard write failed
    }
  }, [tldr, takeaways]);

  return (
    <Card className={`overflow-hidden border shadow-sm ${className}`}>
      <CardHeader className="flex flex-row items-center justify-between pb-3">
        <div className="flex items-center gap-2">
          <div className="flex size-7 items-center justify-center rounded-md bg-primary/10 text-primary">
            <FileText className="size-4" />
          </div>
          <CardTitle className="text-base font-semibold">
            Executive Summary
          </CardTitle>
        </div>

        <Button
          type="button"
          variant="outline"
          size="xs"
          onClick={handleCopy}
          className="gap-1 text-xs"
        >
          {copied ? (
            <>
              <Check className="size-3 text-emerald-600" />
              Copied
            </>
          ) : (
            <>
              <Copy className="size-3" />
              Copy Summary
            </>
          )}
        </Button>
      </CardHeader>

      <CardContent className="flex flex-col gap-4 text-sm">
        {/* TL;DR Callout */}
        <div className="rounded-lg border-l-4 border-l-primary bg-muted/40 p-3 leading-relaxed">
          <span className="font-semibold text-primary block text-xs uppercase tracking-wider mb-1">
            TL;DR
          </span>
          <p className="text-foreground/90">{tldr}</p>
        </div>

        {/* Key Takeaways list */}
        {takeaways && takeaways.length > 0 && (
          <div className="space-y-2">
            <h4 className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
              Key Takeaways
            </h4>
            <ul className="grid gap-2">
              {takeaways.map((takeaway, index) => (
                <li
                  key={index}
                  className="flex items-start gap-2.5 text-xs sm:text-sm text-foreground/90 leading-snug"
                >
                  <CheckCircle2 className="size-4 shrink-0 text-emerald-600 dark:text-emerald-400 mt-0.5" />
                  <span>{takeaway}</span>
                </li>
              ))}
            </ul>
          </div>
        )}
      </CardContent>
    </Card>
  );
}
