from pathlib import Path

from PIL import Image
from transformers import BlipForConditionalGeneration, BlipProcessor

from app.analysis.models import Scene


class FrameCaptioner:
    """Loading BLIP is expensive - wrapped in a class so main.py loads it once and
    reuses it across every frame, instead of reloading per call."""

    def __init__(self, model_id: str = "Salesforce/blip-image-captioning-base"):
        # Deliberately CPU-only, not MPS: transformers' generate() does an isin() call
        # on int64 tensors internally, and MPS only supports isin() on floating types
        # before macOS 14 - crashes on this machine's macOS 13. BLIP-base is small
        # enough that CPU is still fast for a handful of frames.
        self._device = "cpu"
        self._processor = BlipProcessor.from_pretrained(model_id)
        self._model = BlipForConditionalGeneration.from_pretrained(model_id).to(self._device)

    def caption(self, frames: list[tuple[float, Path]]) -> list[Scene]:
        scenes = []
        for timestamp, frame_path in frames:
            image = Image.open(frame_path).convert("RGB")
            inputs = self._processor(image, return_tensors="pt").to(self._device)
            output = self._model.generate(**inputs, max_new_tokens=30)
            description = self._processor.decode(output[0], skip_special_tokens=True)
            scenes.append(Scene(timestamp=timestamp, description=description))
        return scenes
