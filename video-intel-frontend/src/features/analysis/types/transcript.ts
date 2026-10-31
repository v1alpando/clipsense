export interface TranscriptSegment {
  start: number; // in seconds (e.g. 84.5)
  end: number;   // in seconds (e.g. 89.2)
  text: string;
}

export interface TranscriptResponse {
  videoId: string;
  text: string;
  fullText?: string;
  segments: TranscriptSegment[];
}
