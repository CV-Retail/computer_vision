"""ONNX Runtime implementation of the PersonDetector protocol."""

from __future__ import annotations

import time
from pathlib import Path
from typing import Any

import cv2
import numpy as np
import onnxruntime as ort

from kiosk_vision.detection.base import validate_frame
from kiosk_vision.detection.model import BoundingBox, DetectionResult, PersonDetection


class OnnxPersonDetector:
    """Person detector using ONNX Runtime CPU inference."""

    def __init__(
        self,
        model_path: Path | str | None = None,
        confidence_threshold: float = 0.5,
        input_size: tuple[int, int] = (320, 320),
        person_class_id: int = 1,
        session: ort.InferenceSession | None = None,
    ) -> None:
        if not (0.0 < confidence_threshold <= 1.0):
            raise ValueError(
                f"confidence_threshold must be in (0.0, 1.0], got {confidence_threshold}"
            )
        self.confidence_threshold = confidence_threshold
        self.input_size = input_size
        self.person_class_id = person_class_id

        if session is not None:
            self._session = session
        else:
            if model_path is None:
                raise ValueError("model_path must be provided when session is not passed")
            resolved_path = Path(model_path)
            if not resolved_path.is_file():
                raise FileNotFoundError(f"Model file not found: {resolved_path}")

            opts = ort.SessionOptions()
            opts.intra_op_num_threads = 4
            self._session = ort.InferenceSession(
                str(resolved_path),
                sess_options=opts,
                providers=["CPUExecutionProvider"],
            )

        inputs = self._session.get_inputs()
        if not inputs:
            raise ValueError("ONNX model has no input tensors")
        self._input_name = inputs[0].name

    def detect(self, frame: np.ndarray) -> DetectionResult:
        """Execute person detection on input frame."""
        validate_frame(frame)
        start_time = time.perf_counter()

        in_w, in_h = self.input_size
        resized = cv2.resize(frame, (in_w, in_h), interpolation=cv2.INTER_LINEAR)
        rgb = cv2.cvtColor(resized, cv2.COLOR_BGR2RGB)
        normalized = rgb.astype(np.float32) / 255.0
        tensor = np.expand_dims(np.transpose(normalized, (2, 0, 1)), axis=0)

        outputs = self._session.run(None, {self._input_name: tensor})
        detections = self._parse_detections(outputs, in_w, in_h)

        latency_ms = max(0.01, (time.perf_counter() - start_time) * 1000.0)
        return DetectionResult(
            detections=detections,
            count=len(detections),
            latency_ms=latency_ms,
        )

    def _parse_detections(
        self, outputs: list[Any], input_w: int, input_h: int
    ) -> list[PersonDetection]:
        """Parse raw model outputs into filtered PersonDetection objects."""
        if not outputs:
            return []

        # Case 1: Standard Torchvision detection outputs: [boxes, scores, labels]
        if len(outputs) >= 3 and outputs[0].ndim == 2:
            raw_boxes = outputs[0]
            raw_scores = outputs[1]
            raw_labels = outputs[2]

            detections: list[PersonDetection] = []
            for box, score, label in zip(raw_boxes, raw_scores, raw_labels, strict=False):
                score_val = float(score)
                label_val = int(label)
                if label_val != self.person_class_id or score_val < self.confidence_threshold:
                    continue

                norm_box = self._normalize_box(box, input_w, input_h)
                if norm_box is not None:
                    detections.append(PersonDetection(box=norm_box, confidence=score_val))
            return detections

        # Case 2: Single tensor output [1, num_detections, 6] (x1, y1, x2, y2, score, class_id)
        tensor = outputs[0]
        if tensor.ndim == 3 and tensor.shape[0] == 1 and tensor.shape[2] >= 6:
            rows = tensor[0]
            detections = []
            for row in rows:
                score_val = float(row[4])
                label_val = int(row[5])
                if label_val != self.person_class_id or score_val < self.confidence_threshold:
                    continue

                norm_box = self._normalize_box(row[:4], input_w, input_h)
                if norm_box is not None:
                    detections.append(PersonDetection(box=norm_box, confidence=score_val))
            return detections

        return []

    def _normalize_box(self, coords: Any, input_w: int, input_h: int) -> BoundingBox | None:
        """Convert model output coordinates to normalized BoundingBox [0.0, 1.0]."""
        x1, y1, x2, y2 = float(coords[0]), float(coords[1]), float(coords[2]), float(coords[3])

        # If coordinates are in pixel space of the input image, normalize
        if max(x1, y1, x2, y2) > 1.5:
            x1 /= input_w
            x2 /= input_w
            y1 /= input_h
            y2 /= input_h

        # Clamp to [0.0, 1.0]
        x1 = max(0.0, min(1.0, x1))
        y1 = max(0.0, min(1.0, y1))
        x2 = max(0.0, min(1.0, x2))
        y2 = max(0.0, min(1.0, y2))

        if x2 < x1 or y2 < y1:
            return None

        try:
            return BoundingBox(x1=x1, y1=y1, x2=x2, y2=y2)
        except ValueError:
            return None
