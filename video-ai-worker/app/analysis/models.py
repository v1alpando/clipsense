from datetime import datetime, timezone

from pydantic import BaseModel, Field


class VideoMetadata(BaseModel):
    filename: str
    duration_seconds: float
    width: int
    height: int
    fps: float
    codec: str


class TranscriptSegment(BaseModel):
    start: float
    end: float
    text: str


class Transcript(BaseModel):
    language: str
    segments: list[TranscriptSegment]

    @property
    def full_text(self) -> str:
        return " ".join(segment.text.strip() for segment in self.segments)


class Scene(BaseModel):
    timestamp: float
    description: str


class AnalysisResult(BaseModel):
    metadata: VideoMetadata
    transcript: Transcript
    scenes: list[Scene]
    summary: str
    generated_at: datetime = Field(default_factory=lambda: datetime.now(timezone.utc))
