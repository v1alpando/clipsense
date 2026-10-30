from pathlib import Path

from faster_whisper import WhisperModel

from app.analysis.models import Transcript, TranscriptSegment


def transcribe(audio_path: Path, model_size: str = "base") -> Transcript:
    # compute_type="int8" keeps this usable on a CPU-only machine; switch to
    # "float16" if you ever run this with a CUDA GPU available.
    model = WhisperModel(model_size, device="cpu", compute_type="int8")
    segments, info = model.transcribe(str(audio_path))

    transcript_segments = [
        TranscriptSegment(start=segment.start, end=segment.end, text=segment.text)
        for segment in segments
    ]
    return Transcript(language=info.language, segments=transcript_segments)
