import json
import subprocess
from pathlib import Path

from app.analysis.models import VideoMetadata


def extract_metadata(video_path: Path) -> VideoMetadata:
    result = subprocess.run(
        [
            "ffprobe",
            "-v", "error",
            "-print_format", "json",
            "-show_format",
            "-show_streams",
            str(video_path),
        ],
        capture_output=True,
        text=True,
        check=True,
    )
    data = json.loads(result.stdout)
    video_stream = next(s for s in data["streams"] if s["codec_type"] == "video")

    # avg_frame_rate comes as a fraction string like "30000/1001" - it's the reliable
    # field for actual playback fps (r_frame_rate can misreport variable-frame-rate video).
    num, den = video_stream["avg_frame_rate"].split("/")
    fps = float(num) / float(den) if float(den) != 0 else 0.0

    return VideoMetadata(
        filename=video_path.name,
        duration_seconds=float(data["format"]["duration"]),
        width=int(video_stream["width"]),
        height=int(video_stream["height"]),
        fps=fps,
        codec=video_stream["codec_name"],
    )
