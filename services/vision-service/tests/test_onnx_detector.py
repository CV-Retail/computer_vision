"""Unit tests for OnnxPersonDetector with mock inference sessions."""

from unittest.mock import MagicMock

import numpy as np
import pytest

from kiosk_vision.detection.base import PersonDetector
from kiosk_vision.detection.onnx_detector import OnnxPersonDetector


def _create_mock_session(output_return_value: list[np.ndarray]) -> MagicMock:
    """Create a mock ONNX runtime session."""
    session = MagicMock()
    input_node = MagicMock()
    input_node.name = "input"
    session.get_inputs.return_value = [input_node]
    session.run.return_value = output_return_value
    return session


def test_onnx_detector_conforms_to_protocol() -> None:
    session = _create_mock_session([np.zeros((0, 4)), np.zeros(0), np.zeros(0)])
    detector = OnnxPersonDetector(session=session)
    assert isinstance(detector, PersonDetector)


def test_missing_model_file_raises_error() -> None:
    with pytest.raises(FileNotFoundError, match="Model file not found"):
        OnnxPersonDetector(model_path="non_existent_model_file.onnx")


def test_no_model_and_no_session_raises_error() -> None:
    with pytest.raises(ValueError, match="model_path must be provided"):
        OnnxPersonDetector(model_path=None, session=None)


def test_invalid_confidence_threshold() -> None:
    session = _create_mock_session([])
    with pytest.raises(ValueError, match=r"confidence_threshold must be in \(0\.0, 1\.0\]"):
        OnnxPersonDetector(session=session, confidence_threshold=0.0)

    with pytest.raises(ValueError, match=r"confidence_threshold must be in \(0\.0, 1\.0\]"):
        OnnxPersonDetector(session=session, confidence_threshold=1.1)


def test_invalid_frames_raise_value_error() -> None:
    session = _create_mock_session([np.zeros((0, 4)), np.zeros(0), np.zeros(0)])
    detector = OnnxPersonDetector(session=session)

    with pytest.raises(ValueError, match="Frame must be a numpy.ndarray"):
        detector.detect(None)  # type: ignore[arg-type]

    with pytest.raises(ValueError, match="Frame cannot be empty"):
        detector.detect(np.array([], dtype=np.uint8))

    with pytest.raises(ValueError, match="Frame must have 3 dimensions"):
        detector.detect(np.zeros((100, 100), dtype=np.uint8))

    with pytest.raises(ValueError, match="Frame must have 3 channels"):
        detector.detect(np.zeros((100, 100, 4), dtype=np.uint8))


def test_torchvision_format_detection_and_filtering() -> None:
    # 2 detections: 1 person with 0.85 conf, 1 person with 0.35 conf, 1 car (label 3) with 0.9 conf
    boxes = np.array(
        [
            [32.0, 48.0, 160.0, 240.0],
            [10.0, 10.0, 50.0, 50.0],
            [100.0, 100.0, 200.0, 200.0],
        ],
        dtype=np.float32,
    )
    scores = np.array([0.85, 0.35, 0.90], dtype=np.float32)
    labels = np.array([1, 1, 3], dtype=np.int64)

    session = _create_mock_session([boxes, scores, labels])
    detector = OnnxPersonDetector(session=session, confidence_threshold=0.5, input_size=(320, 320))

    frame = np.zeros((480, 640, 3), dtype=np.uint8)
    result = detector.detect(frame)

    assert result.count == 1
    assert len(result.detections) == 1
    assert result.latency_ms > 0.0

    det = result.detections[0]
    assert pytest.approx(det.confidence) == 0.85
    # Coordinates normalized from 320x320:
    # 32/320 = 0.1, 48/320 = 0.15, 160/320 = 0.5, 240/320 = 0.75
    assert pytest.approx(det.box.x1) == 0.1
    assert pytest.approx(det.box.y1) == 0.15
    assert pytest.approx(det.box.x2) == 0.5
    assert pytest.approx(det.box.y2) == 0.75


def test_zero_detections() -> None:
    boxes = np.zeros((0, 4), dtype=np.float32)
    scores = np.zeros((0,), dtype=np.float32)
    labels = np.zeros((0,), dtype=np.int64)

    session = _create_mock_session([boxes, scores, labels])
    detector = OnnxPersonDetector(session=session, confidence_threshold=0.5)

    frame = np.zeros((480, 640, 3), dtype=np.uint8)
    result = detector.detect(frame)

    assert result.count == 0
    assert result.detections == []
    assert result.latency_ms > 0.0


def test_single_tensor_format() -> None:
    # Shape: (1, 2, 6) -> [x1, y1, x2, y2, score, class_id]
    tensor = np.array(
        [
            [
                [0.1, 0.2, 0.6, 0.8, 0.95, 1.0],
                [0.0, 0.0, 0.5, 0.5, 0.40, 1.0],
            ]
        ],
        dtype=np.float32,
    )
    session = _create_mock_session([tensor])
    detector = OnnxPersonDetector(session=session, confidence_threshold=0.5)

    frame = np.zeros((480, 640, 3), dtype=np.uint8)
    result = detector.detect(frame)

    assert result.count == 1
    assert pytest.approx(result.detections[0].confidence) == 0.95
    assert pytest.approx(result.detections[0].box.x1) == 0.1
    assert pytest.approx(result.detections[0].box.y2) == 0.8
