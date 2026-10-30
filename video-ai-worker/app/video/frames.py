import subprocess
from pathlib import Path


def extract_frames(video_path: Path, output_dir: Path, interval_seconds: float) -> list[tuple[float, Path]]:
    output_dir.mkdir(parents=True, exist_ok=True)
    pattern = output_dir / "frame_%04d.jpg"

    subprocess.run(
        [
            "ffmpeg",
            "-y",
            "-i", str(video_path),
            "-vf", f"fps=1/{interval_seconds}",
            str(pattern),
        ],
        capture_output=True,
        text=True,
        check=True,
    )

    frame_paths = sorted(output_dir.glob("frame_*.jpg"))
    # The fps filter samples uniformly starting at t=0, so frame i is always at
    # i * interval_seconds - no need to parse timing out of ffmpeg's own output.
    return [(index * interval_seconds, path) for index, path in enumerate(frame_paths)]
