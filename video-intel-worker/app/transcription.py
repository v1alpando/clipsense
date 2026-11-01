import logging
import os
import sys
from pathlib import Path
from typing import Any, Optional

from openai import APIStatusError, OpenAI, RateLimitError

from app import config

log = logging.getLogger(__name__)

MAX_PAYLOAD_BYTES = 25 * 1024 * 1024  # 25 MB OpenAI Whisper limit

# Singleton lazy caches so long-running worker reuses loaded models/clients
_openai_client: Optional[OpenAI] = None
_local_whisper_model: Any = None


class AudioPayloadTooLargeError(Exception):
    """Raised when extracted audio still exceeds the 25MB OpenAI limit."""


def _ensure_faster_whisper_importable() -> None:
    """Allow importing faster_whisper from current venv or fallback to video-ai-worker/.venv if needed."""
    try:
        import faster_whisper  # noqa: F401
        return
    except ImportError:
        ai_worker_venv = Path(__file__).resolve().parent.parent.parent / "video-ai-worker" / ".venv" / "lib"
        if ai_worker_venv.exists():
            for site_pkg in ai_worker_venv.glob("python*/site-packages"):
                if str(site_pkg) not in sys.path:
                    sys.path.append(str(site_pkg))


def _get_local_whisper_model(model_size: str = "base") -> Any:
    global _local_whisper_model
    if _local_whisper_model is None:
        _ensure_faster_whisper_importable()
        from faster_whisper import WhisperModel

        log.info("Loading local faster-whisper model '%s' (device=cpu, compute_type=int8)...", model_size)
        _local_whisper_model = WhisperModel(model_size, device="cpu", compute_type="int8")
        log.info("Local faster-whisper model '%s' ready.", model_size)
    return _local_whisper_model


def _get_openai_client() -> OpenAI:
    global _openai_client
    if _openai_client is None:
        _openai_client = OpenAI(api_key=config.OPENAI_API_KEY)
    return _openai_client


def transcribe_local(audio_path: Path, model_size: Optional[str] = None) -> tuple[str, list[dict[str, Any]]]:
    """Transcribe audio locally on CPU using faster-whisper ($0 cost, no 25MB limit)."""
    target_model_size = model_size or config.WHISPER_MODEL_SIZE
    model = _get_local_whisper_model(target_model_size)

    log.info("Transcribing %s with local faster-whisper (%s)...", audio_path.name, target_model_size)
    segments_gen, info = model.transcribe(str(audio_path))

    segments: list[dict[str, Any]] = []
    text_parts: list[str] = []

    for seg in segments_gen:
        cleaned_text = seg.text.strip()
        segments.append({
            "start": round(float(seg.start), 2),
            "end": round(float(seg.end), 2),
            "text": cleaned_text,
        })
        if cleaned_text:
            text_parts.append(cleaned_text)

    full_text = " ".join(text_parts)
    log.info(
        "Local transcription finished (language=%s, segments=%d, chars=%d)",
        getattr(info, "language", "unknown"),
        len(segments),
        len(full_text),
    )
    return full_text, segments


def transcribe_openai(audio_path: Path) -> tuple[str, list[dict[str, Any]]]:
    """Transcribe an audio file using OpenAI Whisper API with verbose JSON response."""
    file_size = os.path.getsize(audio_path)
    if file_size > MAX_PAYLOAD_BYTES:
        raise AudioPayloadTooLargeError(
            f"Audio file size ({file_size / (1024 * 1024):.2f} MB) exceeds "
            f"OpenAI 25MB limit. Chunking or bitrate reduction is required."
        )

    log.info("Sending %s (%.2f MB) to OpenAI Whisper API", audio_path.name, file_size / (1024 * 1024))
    client = _get_openai_client()

    with open(audio_path, "rb") as audio_file:
        response = client.audio.transcriptions.create(
            model="whisper-1",
            file=audio_file,
            response_format="verbose_json",
            timestamp_granularities=["segment"],
        )

    full_text = getattr(response, "text", "") or ""
    raw_segments = getattr(response, "segments", []) or []
    segments: list[dict[str, Any]] = []

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


def _is_quota_or_rate_limit_error(exc: Exception) -> bool:
    if isinstance(exc, RateLimitError):
        return True
    if isinstance(exc, APIStatusError) and exc.status_code in (402, 429):
        return True
    msg = str(exc).lower()
    return any(
        token in msg
        for token in ("429", "quota", "credit_balance_exhausted", "insufficient_quota", "rate_limit", "billing")
    )


def transcribe(audio_path: Path) -> tuple[str, list[dict[str, Any]]]:
    """Route transcription to configured provider with automatic fallback to local faster-whisper."""
    provider = config.TRANSCRIPTION_PROVIDER

    if provider == "local":
        return transcribe_local(audio_path)

    if provider == "openai":
        try:
            return transcribe_openai(audio_path)
        except Exception as exc:
            if _is_quota_or_rate_limit_error(exc) or isinstance(exc, AudioPayloadTooLargeError):
                log.warning(
                    "OpenAI transcription failed (%s). Falling back to local faster-whisper...",
                    exc,
                )
                return transcribe_local(audio_path)
            raise

    log.warning("Unknown TRANSCRIPTION_PROVIDER='%s', defaulting to 'local'", provider)
    return transcribe_local(audio_path)
