# KIO-12 Person detection: design

Implements `spec/KIO-12-deteccion-de-personas/requirements.md` (R1–R9). Scope is basic person detection for the vision service: domain models, detector interface, ONNX Runtime inference adapter, PyTorch export script in `ml/`, configuration, dependencies, and automated tests.

## 1. Overview

The detection module introduces the first perception capability into `services/vision-service`. It processes input frames from camera feeds (represented as standard NumPy BGR arrays from OpenCV), executes deep-learning-based person detection purely on CPU, and produces strongly typed detection objects.

```
computer_vision/
├── ml/
│   ├── export_person_detector.py         # PyTorch -> ONNX export script
│   └── README.md                         # ML models and export instructions
└── services/vision-service/
    ├── pyproject.toml                    # dependencies: numpy, opencv-python-headless, onnxruntime
    ├── src/kiosk_vision/
    │   └── detection/
    │       ├── __init__.py               # exported symbols
    │       ├── model.py                  # BoundingBox, PersonDetection, DetectionResult
    │       ├── base.py                   # PersonDetector protocol
    │       ├── onnx_detector.py          # OnnxPersonDetector implementation
    │       └── stub_detector.py          # StubPersonDetector for tests/mocking
    └── tests/
        ├── test_detection_model.py       # unit tests for BoundingBox and value objects
        └── test_onnx_detector.py         # unit tests for OnnxPersonDetector & StubPersonDetector
```

## 2. Decisions on Open Questions

| Question | Decision | Why |
|---|---|---|
| **Coordinate convention** | Store normalized coordinates `[0.0, 1.0]` as primary in `BoundingBox`, with explicit conversion helpers `to_pixels(w, h)` and `from_pixels(x1, y1, x2, y2, w, h)`. | Normalized coordinates are invariant to camera resolution changes (720p, 1080p, etc.) and frame resizing in downstream tracking and cropping modules. |
| **Model architecture** | `SSDLite320-MobileNetV3` from `torchvision` (COCO pre-trained, class 1 = `person`). | Permissive BSD-3-Clause license; mobile-optimized architecture designed for real-time CPU performance (~20–30 ms on modern x86/ARM CPUs); exportable to ONNX via standard PyTorch tooling. |
| **Model export & separation** | Offline export script in `ml/export_person_detector.py`; runtime in `services/vision-service` uses `onnxruntime`. | Keeps heavy PyTorch dependencies (~1–2 GB) out of the production kiosk container; production container only needs `onnxruntime` (~50 MB) and `opencv-python-headless`. |
| **Test fixtures without heavy weights** | Provide `StubPersonDetector` for fast deterministic testing, plus a minimal dummy ONNX model generated programmatically in tests. | Enables CI and developers to run the full test suite in seconds without requiring multi-megabyte weight downloads during unit test runs. |
| **Input color format** | Detector accepts BGR frames (`numpy.ndarray`), converts internally to RGB for model inference. | OpenCV captures frames in BGR format by default. Accepting BGR directly eliminates repetitive color conversion boilerplate for callers. |

## 3. Data Structures & Interfaces

### 3.1 Domain Models (`model.py` - R1, R4, R8)

```python
from dataclasses import dataclass
import numpy as np

@dataclass(frozen=True)
class BoundingBox:
    """Normalized bounding box coordinates [0.0, 1.0]."""
    x1: float
    y1: float
    x2: float
    y2: float

    def __post_init__(self) -> None:
        if not (0.0 <= self.x1 <= 1.0 and 0.0 <= self.y1 <= 1.0):
            raise ValueError(f"Coordinates must be in [0.0, 1.0], got x1={self.x1}, y1={self.y1}")
        if not (0.0 <= self.x2 <= 1.0 and 0.0 <= self.y2 <= 1.0):
            raise ValueError(f"Coordinates must be in [0.0, 1.0], got x2={self.x2}, y2={self.y2}")
        if self.x2 < self.x1 or self.y2 < self.y1:
            raise ValueError(f"Invalid dimensions: x2 ({self.x2}) < x1 ({self.x1}) or y2 ({self.y2}) < y1 ({self.y1})")

    @property
    def width(self) -> float:
        return self.x2 - self.x1

    @property
    def height(self) -> float:
        return self.y2 - self.y1

    @property
    def area(self) -> float:
        return self.width * self.height

    def to_pixels(self, frame_width: int, frame_height: int) -> tuple[int, int, int, int]:
        """Convert normalized coordinates to absolute integer pixel bounds."""
        if frame_width <= 0 or frame_height <= 0:
            raise ValueError(f"Dimensions must be positive, got width={frame_width}, height={frame_height}")
        px1 = int(round(self.x1 * frame_width))
        py1 = int(round(self.y1 * frame_height))
        px2 = int(round(self.x2 * frame_width))
        py2 = int(round(self.y2 * frame_height))
        return (px1, py1, px2, py2)

    @classmethod
    def from_pixels(cls, px1: int, py1: int, px2: int, py2: int, frame_width: int, frame_height: int) -> "BoundingBox":
        """Factory creating BoundingBox from pixel coordinates."""
        if frame_width <= 0 or frame_height <= 0:
            raise ValueError(f"Dimensions must be positive, got width={frame_width}, height={frame_height}")
        return cls(
            x1=max(0.0, min(1.0, px1 / frame_width)),
            y1=max(0.0, min(1.0, py1 / frame_height)),
            x2=max(0.0, min(1.0, px2 / frame_width)),
            y2=max(0.0, min(1.0, py2 / frame_height)),
        )

@dataclass(frozen=True)
class PersonDetection:
    """Individual person detection with confidence score."""
    box: BoundingBox
    confidence: float

    def __post_init__(self) -> None:
        if not (0.0 <= self.confidence <= 1.0):
            raise ValueError(f"Confidence must be in [0.0, 1.0], got {self.confidence}")

@dataclass(frozen=True)
class DetectionResult:
    """Aggregated output of a frame detection pass."""
    detections: list[PersonDetection]
    count: int
    latency_ms: float
```

### 3.2 Detector Protocol (`base.py` - R2)

```python
from typing import Protocol
import numpy as np

class PersonDetector(Protocol):
    """Protocol for person detection backends."""

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
```

## 4. Detection Engines & Components

### 4.1 `OnnxPersonDetector` (`onnx_detector.py` - R3, R4, R7, R8)

- **Initialization:**
  - Loads an ONNX model from `model_path: Path | str`.
  - Configurable parameters:
    - `confidence_threshold: float = 0.5` (validated in `(0.0, 1.0]`).
    - `input_size: tuple[int, int] = (320, 320)`.
    - `iou_threshold: float = 0.45` for NMS if applicable.
  - Initializes `onnxruntime.InferenceSession` with `providers=["CPUExecutionProvider"]`.
  - Configures thread count (`session_options.intra_op_num_threads`) for optimal CPU utilization.
- **Inference flow:**
  1. Validate frame: check `isinstance(frame, np.ndarray)`, `frame.ndim == 3`, `frame.shape[2] == 3`, dimensions > 0.
  2. Record start timestamp `t0 = time.perf_counter()`.
  3. Preprocessing:
     - Resize frame to `(320, 320)` (keeping aspect ratio with letterboxing or direct bilinear interpolation as expected by SSDLite).
     - Convert BGR to RGB (`cv2.cvtColor`).
     - Convert to `float32`, normalize to `[0.0, 1.0]`, transpose to NCHW `(1, 3, 320, 320)`.
  4. Execution:
     - Run `session.run(...)`.
  5. Postprocessing:
     - Extract boxes, scores, and labels.
     - Filter by class label `person` (COCO class ID 1 for SSDLite / class ID 0 for NanoDet).
     - Filter by `score >= confidence_threshold`.
     - Convert boxes back to normalized coordinates `[0.0, 1.0]` relative to original frame dimensions.
  6. Calculate `latency_ms = (time.perf_counter() - t0) * 1000.0`.
  7. Construct and return `DetectionResult(detections=filtered_detections, count=len(filtered_detections), latency_ms=latency_ms)`.
  8. Immediately release references to the input frame (R8.1).

### 4.2 `StubPersonDetector` (`stub_detector.py` - R2, R9)

- In-memory mock detector allowing programmatic definition of detections for unit tests, pipeline integration tests, and environments without ONNX models.
- Can be configured with preset detections or deterministic detection based on frame size/color.

### 4.3 Model Export Pipeline (`ml/export_person_detector.py` - R5)

- Script in `ml/export_person_detector.py`:
  - Uses `torch` and `torchvision.models.detection.ssdlite320_mobilenet_v3_large(weights=SSDLite320_MobileNet_V3_Large_Weights.DEFAULT)`.
  - Sets model to evaluation mode (`model.eval()`).
  - Creates dummy input tensor `torch.randn(1, 3, 320, 320)`.
  - Exports to ONNX:
    ```python
    torch.onnx.export(
        model,
        dummy_input,
        output_path,
        input_names=["input"],
        output_names=["boxes", "scores", "labels"],
        dynamic_axes={"input": {0: "batch_size"}, "boxes": {0: "num_detections"}},
        opset_version=14,
    )
    ```
  - Validates output file with `onnx.checker.check_model`.
  - Verifies license: Torchvision BSD-3-Clause license documentation is confirmed.

## 5. Dependencies and Packaging (`pyproject.toml` - R6)

Updated in `services/vision-service/pyproject.toml`:

```toml
dependencies = [
    "numpy>=1.26.0,<3.0.0",
    "opencv-python-headless>=4.9.0",
    "onnxruntime>=1.17.0",
]
```

All three dependencies have fully permissive commercial licenses:
- `numpy`: BSD-3-Clause
- `opencv-python-headless`: Apache-2.0
- `onnxruntime`: MIT

## 6. Testing Strategy (R9)

### 6.1 Unit Tests (`tests/test_detection_model.py`)
- Test `BoundingBox` validation (valid ranges, negative coordinates, inverted coordinates `x2 < x1`).
- Test coordinate conversions (`to_pixels`, `from_pixels`, rounding, aspect ratios).
- Test `PersonDetection` and `DetectionResult` immutability and confidence validation.

### 6.2 Detector Tests (`tests/test_onnx_detector.py`)
- Test input validation (empty frame, 2D array, 4-channel image, zero width/height).
- Test `StubPersonDetector` behavior with varying confidence thresholds.
- Test `OnnxPersonDetector` with a minimal synthetic ONNX model fixture (validating pre/post-processing, threshold filtering, and latency calculation).
- Test memory release (verifying no retained frame references in detector).

### 6.3 Code Quality and CI
- `ruff check .` with zero errors.
- `ruff format --check .` with zero formatting differences.
- `pytest` passes all tests cleanly on CPU without external GPU/camera dependencies.

## 7. Requirement Coverage

| Requirement | Description | Covered by Design Section |
|---|---|---|
| **R1** | Detection data structures (`BoundingBox`, `PersonDetection`, `DetectionResult`) | Section 3.1 |
| **R2** | Detector interface and contract (`PersonDetector` protocol) | Section 3.2 |
| **R3** | Pretrained model and inference engine (ONNX Runtime, BSD/Apache weights) | Section 2, Section 4.1 |
| **R4** | Confidence thresholding and filtering | Section 3.1, Section 4.1 |
| **R5** | Model export pipeline in `ml/` (PyTorch -> ONNX) | Section 4.3 |
| **R6** | Dependencies and packaging (`pyproject.toml`) | Section 5 |
| **R7** | Performance on CPU (<50 ms, NMS, resizing) | Section 4.1 |
| **R8** | Privacy and memory invariants (no frame persistence, in-RAM only) | Section 3.1, Section 4.1 |
| **R9** | Automated testing and code quality (`pytest`, `ruff`) | Section 6 |

## 8. Risks and Trade-offs

1. **Model Download in CI vs Synthetic Fixture**:
   - *Risk:* Downloading a full 13 MB ONNX model during every CI test run could cause network flakiness.
   - *Mitigation:* Unit tests use `StubPersonDetector` and a tiny synthetic ONNX graph generated in-memory or mock fixtures. The real exported model is used for functional verification and manual smoke tests.
2. **CPU Execution on Constrained Edge Devices**:
   - *Risk:* On very low-end hardware (e.g. older Raspberry Pi 3), SSDLite at 320x320 might achieve 10–15 FPS rather than 20+ FPS.
   - *Mitigation:* The kiosk PC is the primary MVP target (CPU Intel/AMD), where SSDLite comfortably achieves 30–50 FPS. NanoDet-Plus remains an available lightweight drop-in alternative if lighter compute is needed.
