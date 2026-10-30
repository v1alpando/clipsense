from openai import OpenAI

from app import config

_client = OpenAI(api_key=config.OPENAI_API_KEY)


def transcribe(video_path: str) -> str:
    # The endpoint reads mp4/mov/webm/etc. directly - it just pulls the audio track out
    # itself, so no ffmpeg step needed on our side for now. It does cap uploads at 25MB
    # though, so anything longer than a few minutes of video will need splitting or
    # re-encoding to audio-only before this call - not handled yet, first get one short
    # test video working end to end.
    with open(video_path, "rb") as video_file:
        response = _client.audio.transcriptions.create(
            model="whisper-1",
            file=video_file,
        )
    return response.text
