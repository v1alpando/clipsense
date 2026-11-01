export interface HookAnalysis {
  /** Retention score out of 10 for the first 3-5 seconds. */
  score: number;
  /** Type/category of the opening hook (e.g., "Curiosity Gap", "Bold Claim", "Problem / Pain Point"). */
  hookType: string;
  /** In-depth breakdown of what worked and what caused drop-off in the intro. */
  critique: string;
  /** Actionable recommendation to rewrite or re-edit the hook. */
  suggestion: string;
}

export interface KeyMoment {
  id: string;
  /** Timestamp in seconds where the chapter begins. */
  timestamp: number;
  /** Chapter title or topic name. */
  title: string;
  /** Short summary of what is discussed in this chapter. */
  description?: string;
  /** Duration of this chapter in seconds. */
  duration?: number;
}

export interface ExecutiveSummary {
  /** TL;DR one-paragraph synthesis of the whole video. */
  tldr: string;
  /** Key bullet takeaways. */
  takeaways: string[];
}

export interface AnalysisData {
  videoId: string;
  summary: ExecutiveSummary;
  hook: HookAnalysis;
  keyMoments: KeyMoment[];
}
