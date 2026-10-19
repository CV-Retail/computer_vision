"""Person and face detection module for kiosk vision."""

from kiosk_vision.detection.base import PersonDetector, validate_frame
from kiosk_vision.detection.model import BoundingBox, DetectionResult, PersonDetection
from kiosk_vision.detection.onnx_detector import OnnxPersonDetector
from kiosk_vision.detection.stub_detector import StubPersonDetector

__all__ = [
    "BoundingBox",
    "DetectionResult",
    "OnnxPersonDetector",
    "PersonDetection",
    "PersonDetector",
    "StubPersonDetector",
    "validate_frame",
]
