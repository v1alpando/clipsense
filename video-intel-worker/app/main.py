import logging
import os
import tempfile

from app import minio_client, transcription
from app.models import AnalysisRequestedMessage, AnalysisResultMessage
from app.queue_client import run_consumer

logging.basicConfig(level=logging.INFO)
log = logging.getLogger("video-intel-worker")


def handle_analysis_requested(message: AnalysisRequestedMessage) -> AnalysisResultMessage:
    log.info("Picked up analysis job %s for video %s", message.job_id, message.video_id)

    with tempfile.TemporaryDirectory() as tmp_dir:
        video_path = os.path.join(tmp_dir, message.storage_ref)
        minio_client.download_video(message.storage_ref, video_path)
        transcript_text = transcription.transcribe(video_path)

    return AnalysisResultMessage(job_id=message.job_id, success=True, transcript_text=transcript_text)


if __name__ == "__main__":
    log.info("Waiting for analysis.requested messages...")
    run_consumer(handle_analysis_requested)
