import subprocess
from pathlib import Path


def has_audio_stream(video_path: Path) -> bool:
    """Not every video has an audio track (screen recordings without narration,
    silent clips, etc). Checking this up front lets the pipeline skip transcription
    cleanly instead of ffmpeg failing with 'Output file does not contain any stream'."""
    result = subprocess.run(
        [
            "ffprobe",
            "-v", "error",
            "-select_streams", "a",
            "-show_entries", "stream=codec_type",
            "-of", "csv=p=0",
            str(video_path),
        ],
        capture_output=True,
        text=True,
        check=True,
    )
    return bool(result.stdout.strip())


def extract_audio(video_path: Path, output_path: Path) -> Path:
    output_path.parent.mkdir(parents=True, exist_ok=True)
    subprocess.run(
        [
            "ffmpeg",
            "-y",  # overwrite output without prompting - we control this path ourselves
            "-i", str(video_path),
            "-vn",  # drop video stream, audio only
            "-acodec", "pcm_s16le",  # uncompressed wav - what faster-whisper reads natively
            "-ar", "16000",  # whisper models are trained on 16kHz audio
            "-ac", "1",  # mono
            str(output_path),
        ],
        capture_output=True,
        text=True,
        check=True,
    )
    return output_path
