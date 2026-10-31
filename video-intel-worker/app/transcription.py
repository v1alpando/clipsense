import logging
import os
from pathlib import Path
from typing import Any

from openai import OpenAI

from app import config

log = logging.getLogger(__name__)

MAX_PAYLOAD_BYTES = 25 * 1024 * 1024  # 25 MB OpenAI Whisper limit
_client = OpenAI(api_key=config.OPENAI_API_KEY)


class AudioPayloadTooLargeError(Exception):
    """Raised when extracted audio still exceeds the 25MB OpenAI limit."""


def transcribe(audio_path: Path) -> tuple[str, list[dict[str, Any]]]:
    """Transcribe an audio file using OpenAI Whisper API with verbose JSON response.

    Returns:
        tuple of (full_transcript_text, segments_list)
        where segments_list contains dicts: {"start": float, "end": float, "text": str}
    """
    file_size = os.path.getsize(audio_path)
    if file_size > MAX_PAYLOAD_BYTES:
        raise AudioPayloadTooLargeError(
            f"Audio file size ({file_size / (1024 * 1024):.2f} MB) exceeds "
            f"OpenAI 25MB limit. Chunking or bitrate reduction is required."
        )

    log.info("Sending %s (%.2f MB) to OpenAI Whisper API", audio_path.name, file_size / (1024 * 1024))

    with open(audio_path, "rb") as audio_file:
        response = _client.audio.transcriptions.create(
            model="whisper-1",
            file=audio_file,
            response_format="verbose_json",
            timestamp_granularities=["segment"],
        )

    full_text = getattr(response, "text", "") or ""

    raw_segments = getattr(response, "segments", []) or []
    segments = []
    for s in raw_segments:
        if isinstance(s, dict):
            segments.append({
                "start": float(s.get("start", 0.0)),
                "end": float(s.get("end", 0.0)),
                "text": str(s.get("text", "")).strip(),
            })
        else:
            segments.append({
                "start": float(getattr(s, "start", 0.0)),
                "end": float(getattr(s, "end", 0.0)),
                "text": str(getattr(s, "text", "")).strip(),
            })

    return full_text, segments
