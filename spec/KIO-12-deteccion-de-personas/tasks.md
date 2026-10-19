# KIO-12 Person detection: tasks

Implements `design.md` for `requirements.md`. Do the tasks in order and tick each one when done. Commands run from `services/vision-service` unless a task says "repo root". Do not commit, push or open a PR until the user asks.

## Tasks

- [x] **T1 Declare runtime dependencies in `pyproject.toml`** (R6.1–R6.3)
  In `services/vision-service/pyproject.toml`, add `numpy>=1.26.0,<3.0.0`, `opencv-python-headless>=4.9.0`, and `onnxruntime>=1.17.0` to `project.dependencies`. Confirm that all dependencies have permissive open-source licenses (BSD-3-Clause, Apache-2.0, MIT).
  *Verify:* run `python -c "import tomllib; tomllib.load(open('pyproject.toml','rb'))"` to verify syntax, then run `pip install -e ".[dev]"` and verify `python -c "import numpy, cv2, onnxruntime"` succeeds without error.

- [x] **T2 Detection domain models and unit tests** (R1.1–R1.7, R4.1, R8.1–R8.3)
  Create `services/vision-service/src/kiosk_vision/detection/model.py` with immutable frozen dataclasses:
  - `BoundingBox`: normalized coordinates `[0.0, 1.0]`, invariant validation (`x2 >= x1`, `y2 >= y1`, bounds checking), `width`, `height`, `area`, `to_pixels(w, h)`, and `from_pixels(x1, y1, x2, y2, w, h)`.
  - `PersonDetection`: `box: BoundingBox` and `confidence: float` with bounds check `[0.0, 1.0]`.
  - `DetectionResult`: `detections: list[PersonDetection]`, `count: int`, and `latency_ms: float`.
  Ensure no references to frame images, embeddings, or persistent tracking IDs are kept.
  Create `services/vision-service/tests/test_detection_model.py` testing bounding box construction, invalid coordinates, conversion to/from pixels, and confidence validation.
  *Verify:* `pytest tests/test_detection_model.py` passes with 100% assertions green.

- [x] **T3 PersonDetector protocol and StubPersonDetector** (R2.1–R2.5, R4.2, R9.1)
  Create `services/vision-service/src/kiosk_vision/detection/base.py` defining the `PersonDetector(Protocol)` with `detect(frame: np.ndarray) -> DetectionResult`.
  Create `services/vision-service/src/kiosk_vision/detection/stub_detector.py` implementing `StubPersonDetector` for testing and deterministic mocking.
  Create `services/vision-service/tests/test_stub_detector.py` verifying protocol adherence, error handling on invalid frames (empty array, invalid dimensions), and confidence filtering.
  *Verify:* `pytest tests/test_stub_detector.py` passes.

- [x] **T4 Model export pipeline in `ml/`** (R3.2, R3.3, R5.1–R5.4)
  Create `ml/export_person_detector.py` that loads `torchvision.models.detection.ssdlite320_mobilenet_v3_large(weights=...)` (BSD-3-Clause), sets evaluation mode, and exports an ONNX model with documented input `(1, 3, 320, 320)` and outputs (`boxes`, `scores`, `labels`).
  Create `ml/README.md` documenting export instructions, input/output tensor specifications, and licensing validation.
  *Verify:* verify script parses (`python -m py_compile ml/export_person_detector.py`).

- [x] **T5 ONNX Runtime detector implementation** (R3.1, R3.4–R3.5, R4.1–R4.3, R7.1–R7.3, R8.1–R8.3)
  Create `services/vision-service/src/kiosk_vision/detection/onnx_detector.py`:
  - `OnnxPersonDetector` implementing `PersonDetector`.
  - Validates model path exists on initialization, raising descriptive errors if missing.
  - Implements input frame validation (rejecting non-3D, empty, or non-NumPy inputs).
  - Preprocesses BGR frames: converts to RGB, resizes to `(320, 320)`, normalizes to float32 NCHW tensor.
  - Executes ONNX Runtime CPU session.
  - Postprocesses outputs: filters for COCO `person` class, filters by `confidence_threshold`, maps coordinates back to normalized `BoundingBox` objects.
  - Calculates latency in milliseconds.
  - Ensures input frames are not retained in detector instance memory.
  - Update `services/vision-service/src/kiosk_vision/detection/__init__.py` exposing public symbols.
  *Verify:* `python -c "from kiosk_vision.detection import BoundingBox, PersonDetection, DetectionResult, PersonDetector, OnnxPersonDetector, StubPersonDetector"` imports cleanly.

- [x] **T6 Detector test suite with synthetic fixtures** (R9.1–R9.2)
  Create `services/vision-service/tests/test_onnx_detector.py`:
  - Test missing model file raises `FileNotFoundError`.
  - Test invalid frame inputs (None, 2D arrays, empty arrays, wrong channel count) raise `ValueError`.
  - Test threshold filtering with synthetic or mock ONNX outputs.
  - Test zero-person detections produce `count=0` and empty detection list.
  - Test latency is computed and non-negative.
  *Verify:* `pytest tests/test_onnx_detector.py` passes on CPU without external GPU or camera hardware.

- [x] **T7 Lint, format, and full test suite** (R9.3, R9.4)
  In `services/vision-service`, run:
  - `ruff check .`
  - `ruff format --check .` (run `ruff format .` if any formatting issues arise)
  - `pytest` across all tests in `tests/`.
  *Verify:* zero lint errors, zero format warnings, all tests pass.

- [x] **T8 Documentation and privacy audit** (R3.2, R6.2, R8.1–R8.3)
  Update `services/vision-service/README.md` with:
  - Description of `kiosk_vision.detection` and its components.
  - Added dependencies and license audit table (NumPy: BSD, OpenCV: Apache 2.0, ONNX Runtime: MIT, SSDLite: BSD-3).
  - Explicit confirmation of privacy rules: all frame processing is purely in-RAM; no frame, crop, or personal data is saved to disk or network.
  Run a repository grep search in `kiosk_vision/detection` to verify no file writing (`open`, `write`, `imwrite`), external sockets, or biometric persistence exist.
  *Verify:* README updated; grep returns no unauthorized I/O operations.

- [x] **T9 Final verification and checklist review**
  From repository root, run `git status` to ensure only expected files are modified/created. Confirm that all acceptance criteria from R1 to R9 in `requirements.md` are covered and verified.
  Wait for user instruction before committing, pushing, or opening a PR.

## Requirement coverage

| Requirement | Tasks |
|---|---|
| **R1** Detection data structures | T2 |
| **R2** Detector interface and contract | T3 |
| **R3** Pretrained model and inference engine | T4, T5, T6, T8 |
| **R4** Confidence thresholding and filtering | T2, T3, T5, T6 |
| **R5** Model export pipeline in `ml/` | T4 |
| **R6** Dependencies and packaging | T1, T8 |
| **R7** Performance on CPU | T5, T6 |
| **R8** Privacy and memory invariants | T2, T5, T8 |
| **R9** Automated testing and code quality | T2, T3, T6, T7 |
