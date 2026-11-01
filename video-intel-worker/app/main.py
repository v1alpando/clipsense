import logging
import os
import tempfile
from pathlib import Path

from app import analysis_generator, audio, config, minio_client, transcription
from app.models import AnalysisRequestedMessage, AnalysisResultMessage
from app.queue_client import run_consumer

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s: %(message)s")
log = logging.getLogger("video-intel-worker")


def handle_analysis_requested(message: AnalysisRequestedMessage) -> AnalysisResultMessage:
    log.info("Picked up analysis job %s for video %s", message.job_id, message.video_id)

    with tempfile.TemporaryDirectory() as tmp_dir:
        tmp_path = Path(tmp_dir)
        # Store with .mp4 extension so ffmpeg / ffprobe can parse containers reliably
        video_path = tmp_path / f"{message.storage_ref}.mp4"
        audio_path = tmp_path / f"{message.storage_ref}.mp3"

        log.info("Downloading video %s from MinIO to %s", message.storage_ref, video_path)
        minio_client.download_video(message.storage_ref, str(video_path))

        try:
            if not audio.has_audio_stream(video_path):
                log.warning("No audio stream found in video %s - generating silent video analysis", message.video_id)
                analysis = analysis_generator.generate_analysis("", [])
                return AnalysisResultMessage(
                    job_id=message.job_id,
                    success=True,
                    transcript_text="",
                    segments=[],
                    analysis=analysis,
                )

            log.info("Extracting lightweight MP3 audio from %s", video_path)
            audio.extract_audio(video_path, audio_path)

            log.info("Transcribing audio %s (provider=%s)", audio_path, config.TRANSCRIPTION_PROVIDER)
            transcript_text, segments = transcription.transcribe(audio_path)

            log.info("Generating deep semantic analysis for video %s", message.video_id)
            analysis = analysis_generator.generate_analysis(transcript_text, segments)

            return AnalysisResultMessage(
                job_id=message.job_id,
                success=True,
                transcript_text=transcript_text,
                segments=segments,
                analysis=analysis,
            )

        except Exception as e:
            log.exception("Failed processing analysis job %s: %s", message.job_id, e)
            return AnalysisResultMessage(
                job_id=message.job_id,
                success=False,
                failure_reason=str(e),
            )
        finally:
            # Explicit cleanup of intermediate files (in addition to TemporaryDirectory context manager)
            for file_path in (audio_path, video_path):
                if file_path.exists():
                    try:
                        file_path.unlink()
                    except OSError as cleanup_err:
                        log.warning("Could not delete temporary file %s: %s", file_path, cleanup_err)


if __name__ == "__main__":
    log.info("Waiting for analysis.requested messages...")
    run_consumer(handle_analysis_requested)
