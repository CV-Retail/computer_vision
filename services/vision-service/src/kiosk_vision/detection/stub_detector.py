"""Stub person detector implementation for testing and mocking."""

import time

import numpy as np

from kiosk_vision.detection.base import validate_frame
from kiosk_vision.detection.model import DetectionResult, PersonDetection


class StubPersonDetector:
    """Configurable stub detector returning preset detections without deep learning inference."""

    def __init__(
        self,
        preset_detections: list[PersonDetection] | None = None,
        confidence_threshold: float = 0.5,
    ) -> None:
        if not (0.0 < confidence_threshold <= 1.0):
            raise ValueError(
                f"confidence_threshold must be in (0.0, 1.0], got {confidence_threshold}"
            )
        self._preset_detections = list(preset_detections or [])
        self.confidence_threshold = confidence_threshold

    def set_preset_detections(self, detections: list[PersonDetection]) -> None:
        """Update preset detections."""
        self._preset_detections = list(detections)

    def detect(self, frame: np.ndarray) -> DetectionResult:
        """Simulate detection on a frame."""
        validate_frame(frame)
        start_time = time.perf_counter()

        filtered = [
            det for det in self._preset_detections if det.confidence >= self.confidence_threshold
        ]

        latency_ms = max(0.01, (time.perf_counter() - start_time) * 1000.0)
        return DetectionResult(
            detections=filtered,
            count=len(filtered),
            latency_ms=latency_ms,
        )
