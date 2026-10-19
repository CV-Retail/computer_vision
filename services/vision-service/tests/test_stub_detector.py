"""Unit tests for PersonDetector protocol and StubPersonDetector."""

import numpy as np
import pytest

from kiosk_vision.detection.base import PersonDetector, validate_frame
from kiosk_vision.detection.model import BoundingBox, DetectionResult, PersonDetection
from kiosk_vision.detection.stub_detector import StubPersonDetector


def test_stub_detector_conforms_to_protocol() -> None:
    detector = StubPersonDetector()
    assert isinstance(detector, PersonDetector)


def test_validate_frame_valid() -> None:
    valid_frame = np.zeros((480, 640, 3), dtype=np.uint8)
    # Should not raise
    validate_frame(valid_frame)


def test_validate_frame_invalid_type() -> None:
    with pytest.raises(ValueError, match="Frame must be a numpy.ndarray"):
        validate_frame([[0, 0, 0]])  # type: ignore[arg-type]


def test_validate_frame_empty() -> None:
    with pytest.raises(ValueError, match="Frame cannot be empty"):
        validate_frame(np.array([], dtype=np.uint8))


def test_validate_frame_wrong_ndim() -> None:
    with pytest.raises(ValueError, match="Frame must have 3 dimensions"):
        validate_frame(np.zeros((100, 100), dtype=np.uint8))


def test_validate_frame_wrong_channels() -> None:
    with pytest.raises(ValueError, match="Frame must have 3 channels"):
        validate_frame(np.zeros((100, 100, 4), dtype=np.uint8))


def test_stub_detector_invalid_threshold() -> None:
    with pytest.raises(ValueError, match=r"confidence_threshold must be in \(0\.0, 1\.0\]"):
        StubPersonDetector(confidence_threshold=0.0)

    with pytest.raises(ValueError, match=r"confidence_threshold must be in \(0\.0, 1\.0\]"):
        StubPersonDetector(confidence_threshold=1.5)


def test_stub_detector_empty_detections() -> None:
    detector = StubPersonDetector()
    frame = np.zeros((100, 100, 3), dtype=np.uint8)
    result = detector.detect(frame)
    assert isinstance(result, DetectionResult)
    assert result.count == 0
    assert result.detections == []
    assert result.latency_ms >= 0.0


def test_stub_detector_filters_by_confidence_threshold() -> None:
    box = BoundingBox(x1=0.1, y1=0.1, x2=0.5, y2=0.5)
    det1 = PersonDetection(box=box, confidence=0.8)
    det2 = PersonDetection(box=box, confidence=0.4)
    det3 = PersonDetection(box=box, confidence=0.6)

    detector = StubPersonDetector(
        preset_detections=[det1, det2, det3],
        confidence_threshold=0.5,
    )
    frame = np.zeros((100, 100, 3), dtype=np.uint8)
    result = detector.detect(frame)

    assert result.count == 2
    assert result.detections == [det1, det3]


def test_stub_detector_set_preset_detections() -> None:
    detector = StubPersonDetector(confidence_threshold=0.5)
    frame = np.zeros((100, 100, 3), dtype=np.uint8)
    assert detector.detect(frame).count == 0

    box = BoundingBox(x1=0.2, y1=0.2, x2=0.8, y2=0.8)
    det = PersonDetection(box=box, confidence=0.9)
    detector.set_preset_detections([det])

    result = detector.detect(frame)
    assert result.count == 1
    assert result.detections == [det]
