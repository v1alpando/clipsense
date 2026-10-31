import json
import logging
import os
import shutil
import subprocess
from pathlib import Path

log = logging.getLogger(__name__)

# Search for ffmpeg/ffprobe in PATH or in local conda environment
def _find_binary(name: str) -> str:
    path = shutil.which(name)
    if path:
        return path
    # Check conda ffmpeg in video-ai-worker if running locally
    local_conda_path = Path(__file__).resolve().parent.parent.parent / "video-ai-worker" / ".conda-ffmpeg" / "bin" / name
    if local_conda_path.exists():
        return str(local_conda_path)
    return name


FFMPEG_BIN = os.getenv("FFMPEG_BIN", _find_binary("ffmpeg"))
FFPROBE_BIN = os.getenv("FFPROBE_BIN", _find_binary("ffprobe"))
SUBPROCESS_TIMEOUT_SECONDS = int(os.getenv("FFMPEG_TIMEOUT_SECONDS", "180"))


def has_audio_stream(video_path: Path) -> bool:
    """Check if the container contains at least one audio stream."""
    try:
        result = subprocess.run(
            [
                FFPROBE_BIN,
                "-v", "error",
                "-select_streams", "a",
                "-show_entries", "stream=codec_type",
                "-of", "csv=p=0",
                str(video_path),
            ],
            capture_output=True,
            text=True,
            check=True,
            timeout=SUBPROCESS_TIMEOUT_SECONDS,
        )
        return bool(result.stdout.strip())
    except subprocess.CalledProcessError as e:
        log.warning("ffprobe failed on %s: %s", video_path, e.stderr)
        return False
    except Exception as e:
        log.warning("Failed to inspect audio stream on %s: %s", video_path, e)
        return False


def extract_audio(video_path: Path, output_path: Path, bitrate_kbps: int = 64) -> Path:
    """Extract audio track and re-encode to lightweight mono MP3 at 16kHz.

    16kHz mono MP3 at 64 kbps produces ~0.48 MB per minute, allowing up to
    ~50 minutes of audio within OpenAI's 25MB limit while preserving Whisper accuracy.
    """
    output_path.parent.mkdir(parents=True, exist_ok=True)
    subprocess.run(
        [
            FFMPEG_BIN,
            "-y",
            "-i", str(video_path),
            "-vn",
            "-acodec", "libmp3lame",
            "-b:a", f"{bitrate_kbps}k",
            "-ar", "16000",
            "-ac", "1",
            str(output_path),
        ],
        capture_output=True,
        text=True,
        check=True,
        timeout=SUBPROCESS_TIMEOUT_SECONDS,
    )
    return output_path
