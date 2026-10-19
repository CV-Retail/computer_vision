"""Unit tests for detection domain data structures."""

import pytest

from kiosk_vision.detection.model import BoundingBox, DetectionResult, PersonDetection


def test_bounding_box_valid_construction() -> None:
    box = BoundingBox(x1=0.1, y1=0.2, x2=0.5, y2=0.8)
    assert box.x1 == 0.1
    assert box.y1 == 0.2
    assert box.x2 == 0.5
    assert box.y2 == 0.8
    assert pytest.approx(box.width) == 0.4
    assert pytest.approx(box.height) == 0.6
    assert pytest.approx(box.area) == 0.24


def test_bounding_box_boundary_extremes() -> None:
    box = BoundingBox(x1=0.0, y1=0.0, x2=1.0, y2=1.0)
    assert box.width == 1.0
    assert box.height == 1.0
    assert box.area == 1.0


def test_bounding_box_rejects_negative_coordinates() -> None:
    with pytest.raises(ValueError, match=r"Coordinates must be in \[0\.0, 1\.0\]"):
        BoundingBox(x1=-0.1, y1=0.2, x2=0.5, y2=0.8)

    with pytest.raises(ValueError, match=r"Coordinates must be in \[0\.0, 1\.0\]"):
        BoundingBox(x1=0.1, y1=-0.05, x2=0.5, y2=0.8)


def test_bounding_box_rejects_coordinates_greater_than_one() -> None:
    with pytest.raises(ValueError, match=r"Coordinates must be in \[0\.0, 1\.0\]"):
        BoundingBox(x1=0.1, y1=0.2, x2=1.1, y2=0.8)

    with pytest.raises(ValueError, match=r"Coordinates must be in \[0\.0, 1\.0\]"):
        BoundingBox(x1=0.1, y1=0.2, x2=0.5, y2=1.05)


def test_bounding_box_rejects_inverted_dimensions() -> None:
    with pytest.raises(ValueError, match=r"Invalid dimensions: x2 .* < x1"):
        BoundingBox(x1=0.6, y1=0.2, x2=0.4, y2=0.8)

    with pytest.raises(ValueError, match=r"Invalid dimensions: .* y2 .* < y1"):
        BoundingBox(x1=0.1, y1=0.9, x2=0.5, y2=0.3)


def test_bounding_box_to_pixels() -> None:
    box = BoundingBox(x1=0.1, y1=0.2, x2=0.5, y2=0.8)
    px1, py1, px2, py2 = box.to_pixels(frame_width=640, frame_height=480)
    assert px1 == 64
    assert py1 == 96
    assert px2 == 320
    assert py2 == 384


def test_bounding_box_to_pixels_invalid_frame_dimensions() -> None:
    box = BoundingBox(x1=0.1, y1=0.2, x2=0.5, y2=0.8)
    with pytest.raises(ValueError, match="Dimensions must be positive"):
        box.to_pixels(frame_width=0, frame_height=480)
    with pytest.raises(ValueError, match="Dimensions must be positive"):
        box.to_pixels(frame_width=640, frame_height=-10)


def test_bounding_box_from_pixels() -> None:
    box = BoundingBox.from_pixels(
        px1=64, py1=96, px2=320, py2=384, frame_width=640, frame_height=480
    )
    assert pytest.approx(box.x1) == 0.1
    assert pytest.approx(box.y1) == 0.2
    assert pytest.approx(box.x2) == 0.5
    assert pytest.approx(box.y2) == 0.8


def test_bounding_box_from_pixels_invalid() -> None:
    with pytest.raises(ValueError, match="Dimensions must be positive"):
        BoundingBox.from_pixels(px1=10, py1=10, px2=50, py2=50, frame_width=0, frame_height=100)
    with pytest.raises(ValueError, match="Invalid pixel bounds"):
        BoundingBox.from_pixels(px1=50, py1=10, px2=20, py2=50, frame_width=100, frame_height=100)


def test_person_detection_valid() -> None:
    box = BoundingBox(x1=0.1, y1=0.1, x2=0.4, y2=0.8)
    detection = PersonDetection(box=box, confidence=0.85)
    assert detection.box == box
    assert detection.confidence == 0.85


def test_person_detection_invalid_confidence() -> None:
    box = BoundingBox(x1=0.1, y1=0.1, x2=0.4, y2=0.8)
    with pytest.raises(ValueError, match=r"Confidence must be in \[0\.0, 1\.0\]"):
        PersonDetection(box=box, confidence=-0.01)
    with pytest.raises(ValueError, match=r"Confidence must be in \[0\.0, 1\.0\]"):
        PersonDetection(box=box, confidence=1.01)


def test_detection_result_valid() -> None:
    box = BoundingBox(x1=0.1, y1=0.1, x2=0.4, y2=0.8)
    det = PersonDetection(box=box, confidence=0.92)
    result = DetectionResult(detections=[det], count=1, latency_ms=15.4)
    assert result.count == 1
    assert len(result.detections) == 1
    assert result.latency_ms == 15.4


def test_detection_result_empty() -> None:
    result = DetectionResult(detections=[], count=0, latency_ms=5.0)
    assert result.count == 0
    assert result.detections == []
    assert result.latency_ms == 5.0


def test_detection_result_mismatched_count() -> None:
    box = BoundingBox(x1=0.1, y1=0.1, x2=0.4, y2=0.8)
    det = PersonDetection(box=box, confidence=0.92)
    with pytest.raises(ValueError, match="Count .* does not match"):
        DetectionResult(detections=[det], count=2, latency_ms=10.0)
