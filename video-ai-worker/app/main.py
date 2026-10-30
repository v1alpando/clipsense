import logging
import os
import sys
from pathlib import Path

from dotenv import load_dotenv

from app.analysis.combiner import combine
from app.analysis.models import Transcript
from app.transcription.whisper import transcribe
from app.video.audio import extract_audio, has_audio_stream
from app.video.frames import extract_frames
from app.video.metadata import extract_metadata
from app.vision.captioner import FrameCaptioner

load_dotenv()

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
log = logging.getLogger("video-ai-worker")

WORK_DIR = Path(os.getenv("WORK_DIR", "./work"))
WHISPER_MODEL_SIZE = os.getenv("WHISPER_MODEL_SIZE", "base")
VISION_MODEL_ID = os.getenv("VISION_MODEL_ID", "Salesforce/blip-image-captioning-base")
FRAME_INTERVAL_SECONDS = float(os.getenv("FRAME_INTERVAL_SECONDS", "2.0"))


def process_video(video_path: Path):
    run_dir = WORK_DIR / video_path.stem
    run_dir.mkdir(parents=True, exist_ok=True)

    log.info("Extracting metadata from %s", video_path)
    metadata = extract_metadata(video_path)
    log.info(
        "duration=%.1fs %dx%d fps=%.2f codec=%s",
        metadata.duration_seconds, metadata.width, metadata.height, metadata.fps, metadata.codec,
    )

    if has_audio_stream(video_path):
        audio_path = run_dir / "audio.wav"
        log.info("Extracting audio -> %s", audio_path)
        extract_audio(video_path, audio_path)

        log.info("Transcribing with faster-whisper (%s)", WHISPER_MODEL_SIZE)
        transcript = transcribe(audio_path, model_size=WHISPER_MODEL_SIZE)
        log.info("Transcript language=%s, %d segments", transcript.language, len(transcript.segments))
    else:
        # Not an error - plenty of real videos are silent (screen recordings, clips
        # with no narration). An empty transcript is a valid, expected result here,
        # not a failure that should stop the pipeline.
        log.warning("No audio stream detected - skipping transcription")
        transcript = Transcript(language="unknown", segments=[])

    frames_dir = run_dir / "frames"
    log.info("Extracting frames every %.1fs -> %s", FRAME_INTERVAL_SECONDS, frames_dir)
    frames = extract_frames(video_path, frames_dir, FRAME_INTERVAL_SECONDS)
    log.info("Extracted %d frames", len(frames))

    log.info("Captioning frames with %s", VISION_MODEL_ID)
    captioner = FrameCaptioner(VISION_MODEL_ID)
    scenes = captioner.caption(frames)
    for scene in scenes:
        log.info("  [%.1fs] %s", scene.timestamp, scene.description)

    return combine(metadata, transcript, scenes)


if __name__ == "__main__":
    if len(sys.argv) != 2:
        print("Usage: python -m app.main <path-to-video.mp4>")
        sys.exit(1)

    result = process_video(Path(sys.argv[1]))
    print(result.model_dump_json(indent=2))
