from dataclasses import dataclass
from typing import Any, Optional


@dataclass
class AnalysisRequestedMessage:
    """Mirrors io.videointel.analysis.domain.AnalysisRequestedMessage (Java record)."""

    job_id: str
    video_id: str
    storage_ref: str

    @staticmethod
    def from_dict(data: dict[str, Any]) -> "AnalysisRequestedMessage":
        return AnalysisRequestedMessage(
            job_id=data["jobId"],
            video_id=data["videoId"],
            storage_ref=data["storageRef"],
        )


@dataclass
class AnalysisResultMessage:
    """Mirrors io.videointel.analysis.infrastructure.AnalysisResultMessage (Java record)."""

    job_id: str
    success: bool
    transcript_text: Optional[str] = None
    failure_reason: Optional[str] = None
    segments: Optional[list[dict[str, Any]]] = None
    analysis: Optional[dict[str, Any]] = None

    def to_dict(self) -> dict[str, Any]:
        data: dict[str, Any] = {
            "jobId": self.job_id,
            "success": self.success,
            "transcriptText": self.transcript_text,
            "failureReason": self.failure_reason,
        }
        if self.segments is not None:
            data["segments"] = self.segments
        if self.analysis is not None:
            data["analysis"] = self.analysis
        return data
