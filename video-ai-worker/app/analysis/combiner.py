from app.analysis.models import AnalysisResult, Scene, Transcript, VideoMetadata


def combine(metadata: VideoMetadata, transcript: Transcript, scenes: list[Scene]) -> AnalysisResult:
    summary = _naive_summary(transcript, scenes)
    return AnalysisResult(metadata=metadata, transcript=transcript, scenes=scenes, summary=summary)


def _naive_summary(transcript: Transcript, scenes: list[Scene]) -> str:
    # Placeholder until a local LLM is wired in - no OpenAI, no cloud calls, nothing
    # billed. This just proves the combine step has both transcript and scenes
    # available at once; real summarization is a separate later task.
    words = transcript.full_text.split()
    if not words:
        return f"{len(scenes)} scenes analyzed, no speech detected."
    preview = " ".join(words[:30]) + ("..." if len(words) > 30 else "")
    return f"{len(scenes)} scenes analyzed. Speech preview: {preview}"
