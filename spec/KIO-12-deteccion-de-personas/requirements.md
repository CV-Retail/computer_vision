# KIO-12 Person detection: requirements

## Introduction

In the smart kiosk system, the vision service (`services/vision-service`) captures camera frames, detects people in front of the screen, tracks them, estimates anonymous demographic attributes (age ranges, gender, expression), and publishes audience events to the local event bus (see `docs/kiosko-arquitectura-mvp.md`).

This feature, **KIO-12 Person detection**, implements the foundation of the vision perception pipeline: identifying human presence in a video frame. The detector determines where persons are located within the camera's field of view, returning their bounding boxes, detection confidence, and the total count.

Goals:
- Establish the `kiosk_vision.detection` module with clean domain abstractions and detector interfaces.
- Integrate a lightweight, real-time person detector running purely on CPU (Ubuntu PC and future ARM64/Raspberry Pi).
- Define the export and validation workflow from PyTorch (`ml/`) to ONNX Runtime (`services/vision-service`), adhering strictly to permissive licenses (BSD-3-Clause, Apache-2.0, MIT) and prohibiting AGPL (YOLO/Ultralytics) and non-commercial weights.
- Ensure strict privacy invariants: video frames are processed in memory and discarded immediately; no identifiable biometric templates, face crops, or bounding boxes are persisted to disk or sent over network events.
- Provide comprehensive automated tests using synthetic test images and fixtures with `pytest` and `ruff`.

Source: Trello card KIO-12, `docs/kiosko-arquitectura-mvp.md`, and `AGENTS.md`.

## Requirements

### R1 Detection data structures

**User story:** As a developer of downstream vision modules (tracking and attribute estimation), I want standard data structures representing person detections so that subsequent pipeline stages receive uniform, strongly typed outputs.

Acceptance criteria:

- R1.1 The system shall provide an immutable `BoundingBox` value object representing rectangular spatial coordinates (`x1`, `y1`, `x2`, `y2`) within a frame.
- R1.2 If a `BoundingBox` is created with negative coordinates, or with `x2 < x1` or `y2 < y1`, then the system shall reject it with a validation error.
- R1.3 The `BoundingBox` shall support conversion between normalized coordinates (floats in range `[0.0, 1.0]`) and absolute pixel coordinates given frame dimensions `(width, height)`.
- R1.4 The system shall provide a `PersonDetection` data structure containing at least: `box` (`BoundingBox`) and `confidence` (float in range `[0.0, 1.0]`).
- R1.5 If a `PersonDetection` is created with a confidence outside `[0.0, 1.0]`, then the system shall reject it with a validation error.
- R1.6 The system shall provide a `DetectionResult` data structure containing the list of detected persons, the count of persons, and the inference latency in milliseconds.
- R1.7 The detection structures shall not contain personal identifiers, embeddings, tracking IDs, or references to raw image frames.

### R2 Detector interface and contract

**User story:** As a software engineer, I want an abstract detector interface so that the vision service can swap detection engines (e.g. ONNX Runtime, OpenCV DNN, or test mocks) without changing client code.

Acceptance criteria:

- R2.1 The system shall define a `PersonDetector` protocol or abstract base class in `kiosk_vision.detection`.
- R2.2 The `PersonDetector` shall declare a `detect(frame: np.ndarray) -> DetectionResult` method.
- R2.3 When an empty or invalid frame (e.g., zero dimensions or invalid shape) is passed to `detect`, the system shall raise a descriptive `ValueError`.
- R2.4 When a frame with no persons is analyzed, the system shall return a `DetectionResult` with an empty detections list and count equal to 0.
- R2.5 While the detector is active, all frame buffers passed to `detect` shall be released from memory after inference and never retained in internal detector state.

### R3 Pretrained model and inference engine

**User story:** As a kiosk operator, I want person detection to execute efficiently on commodity CPU hardware using pre-trained models with commercial-friendly licenses.

Acceptance criteria:

- R3.1 The detection engine shall use an ONNX model executed via ONNX Runtime (`onnxruntime`) or OpenCV DNN (`cv2.dnn`) on CPU.
- R3.2 The model weights shall originate from a model with a verified permissive commercial license (BSD-3-Clause, Apache-2.0, or MIT); the system shall not use YOLO/Ultralytics (AGPL) or any research-only/non-commercial weights.
- R3.3 The default model shall be based on a mobile-optimized architecture (such as SSDLite-MobileNetV3 or NanoDet-Plus) trained on COCO object classes, extracting detections where class is `person`.
- R3.4 When initialized, the detector shall load the ONNX model from a specified file path or package resource without requiring network access.
- R3.5 If the specified model file does not exist or is corrupted, then the detector initialization shall raise a descriptive error indicating the failure.

### R4 Confidence thresholding and filtering

**User story:** As a kiosk integrator, I want to configure the minimum confidence threshold so that false positives can be minimized in retail environments.

Acceptance criteria:

- R4.1 The `PersonDetector` shall accept a configurable confidence threshold `confidence_threshold` (float in range `(0.0, 1.0]`), defaulting to a sensible baseline (e.g., 0.5).
- R4.2 When `detect` is executed, the system shall exclude all person detections whose confidence score is below the configured `confidence_threshold`.
- R4.3 If a `confidence_threshold` outside `(0.0, 1.0]` is configured, then the detector shall reject it with a configuration error.

### R5 Model export pipeline in `ml/`

**User story:** As an ML engineer, I want a reproducible export script in `ml/` that converts pre-trained PyTorch/Torchvision detection models to ONNX format, so that training and runtime dependencies remain decoupled.

Acceptance criteria:

- R5.1 The `ml/` directory shall contain a standalone export script (e.g. `ml/export_person_detector.py`) that exports a pre-trained PyTorch/Torchvision model (such as `ssdlite320_mobilenet_v3_large`) to ONNX.
- R5.2 When the export script runs, the system shall generate a valid `.onnx` model file with standard input shapes (e.g. `1x3x320x320`) and documented output tensors (bounding boxes, scores, class labels).
- R5.3 The export script shall verify that output tensor dimensions and confidence ranges match the expectations of `kiosk_vision.detection`.
- R5.4 The runtime package `services/vision-service` shall not depend on `torch` or `torchvision`; PyTorch dependencies shall remain isolated to `ml/`.

### R6 Dependencies and packaging

**User story:** As a developer, I want runtime dependencies declared in `pyproject.toml` so that the vision service installs cleanly in Docker and local environments.

Acceptance criteria:

- R6.1 `services/vision-service/pyproject.toml` shall declare runtime dependencies needed for detection: `numpy`, `opencv-python-headless` (or `opencv-python`), and `onnxruntime`.
- R6.2 All added dependencies shall have permissive open-source licenses compatible with commercial deployment (BSD, MIT, Apache-2.0).
- R6.3 When `pip install -e ".[dev]"` is executed in `services/vision-service`, all dependencies shall install without version conflicts on Python 3.11.

### R7 Performance on CPU

**User story:** As an operator, I want person detection to maintain sufficient throughput on CPU so that audience presence is recognized in near real-time.

Acceptance criteria:

- R7.1 On standard x86_64 CPU hardware, person detection on a single frame shall complete within an average latency of 50 ms (equivalent to >= 20 FPS).
- R7.2 The preprocessing step shall resize and normalize input frames to the model's required input resolution while preserving aspect ratio (e.g., through letterboxing or direct scaling as required by the model architecture).
- R7.3 The postprocessing step shall apply Non-Maximum Suppression (NMS) to eliminate duplicate overlapping bounding boxes for the same individual.

### R8 Privacy and memory invariants

**User story:** As a retail customer, I want the system to ensure that no video frames, facial features, or personal tracking records are saved or exposed, ensuring total anonymity.

Acceptance criteria:

- R8.1 The detection module shall operate strictly in RAM; it shall not write image frames, crops, or embeddings to disk, database, or external network connections.
- R8.2 Logging statements shall record only aggregate counts and timing metrics (e.g., "detected 2 persons in 24 ms"), never raw coordinates or sensitive biometric information.
- R8.3 The detection outputs shall contain bounding boxes only for internal in-memory use (for tracking in KIO-14 and attribute cropping in KIO-15); they shall not be published to the Redis event bus.

### R9 Automated testing and code quality

**User story:** As a developer, I want comprehensive unit tests and automated linting so that the detection module is verifiable without physical camera hardware.

Acceptance criteria:

- R9.1 The system shall provide unit tests under `services/vision-service/tests/` exercising:
  - Bounding box validation, normalization, and invalid coordinate rejection.
  - Person detection and result data structures.
  - Detector behavior on empty frames, zero-detection frames, and synthetic test frames.
  - Confidence threshold filtering.
- R9.2 The unit tests shall execute and pass under `pytest` without requiring physical camera devices or GPU hardware.
- R9.3 When `ruff check .` runs in `services/vision-service`, the system shall report zero violations.
- R9.4 When `ruff format --check .` runs in `services/vision-service`, the system shall report zero formatting differences.

## Out of scope

- Physical camera frame acquisition (handled in KIO-10 / `ICameraSource`).
- Face detection and facial bounding boxes (KIO-13).
- Multi-frame temporal tracking and dwell time accumulation (KIO-14 / ByteTrack).
- Demographic attribute estimation: age, gender, facial expression (KIO-15, KIO-16, KIO-17).
- Audience event generation and Redis pub/sub transmission (KIO-20).
- Cloud storage or remote telemetry.

## Open questions (to settle in design.md)

1. **Coordinate convention:** Should `BoundingBox` expose coordinates normalized to `[0.0, 1.0]` as primary, or absolute pixels `[0, width/height]`, or provide seamless properties for both?
2. **Default input resolution:** SSDLite320 uses `320x320`, whereas NanoDet-Plus typically uses `320x320` or `416x416`. Which input resolution provides the optimal speed/accuracy trade-off for kiosk distances (1–3 meters)?
3. **ONNX model storage & distribution:** Where should the default `.onnx` weight file be stored for runtime (e.g., packaged in `kiosk_vision/detection/models/`, downloaded via a setup script, or mounted via Docker volume)?
4. **Mock detector for headless testing:** Should a `StubPersonDetector` or `HOGPersonDetector` be provided alongside `OnnxPersonDetector` to facilitate fast testing in CI environments where ONNX weights might not be downloaded?
