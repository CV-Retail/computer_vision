"""Protocol and base utilities for person detectors."""

from typing import Protocol, runtime_checkable

import numpy as np

from kiosk_vision.detection.model import DetectionResult


def validate_frame(frame: np.ndarray) -> None:
    """Validate that input frame is a non-empty 3-channel (BGR) numpy array.

    Raises:
        ValueError: If frame is invalid, empty, or has incorrect dimensions.
    """
    if not isinstance(frame, np.ndarray):
        raise ValueError(f"Frame must be a numpy.ndarray, got {type(frame).__name__}")
    if frame.size == 0:
        raise ValueError("Frame cannot be empty")
    if frame.ndim != 3:
        raise ValueError(f"Frame must have 3 dimensions (H, W, C), got {frame.ndim}")
    if frame.shape[2] != 3:
        raise ValueError(f"Frame must have 3 channels (BGR), got {frame.shape[2]}")
    if frame.shape[0] <= 0 or frame.shape[1] <= 0:
        raise ValueError(f"Frame dimensions must be positive, got {frame.shape[:2]}")


@runtime_checkable
class PersonDetector(Protocol):
    """Protocol for person detection engines."""

    def detect(self, frame: np.ndarray) -> DetectionResult:
        """Detect people in the provided BGR image frame.

        Args:
            frame: 3D NumPy array of shape (height, width, channels) in BGR format.

        Returns:
            DetectionResult with detected bounding boxes, count, and latency.

        Raises:
            ValueError: If the frame is invalid, empty, or incorrectly shaped.
        """
        ...
