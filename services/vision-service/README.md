# kiosk-vision-service

Vision service of the smart kiosk. It will capture camera frames, detect people and faces, track them, estimate age, gender and expression, and publish anonymous audience events. This version is only the skeleton (KIO-6): a package that installs, passes lint and tests, builds into an image and starts under Docker Compose. It has no runtime dependencies and does no camera, model or network work yet.

Design: [`spec/KIO-6-vision-skeleton/`](../../spec/KIO-6-vision-skeleton/) and [`docs/kiosko-arquitectura-mvp.md`](../../docs/kiosko-arquitectura-mvp.md).

## Layout

```
src/kiosk_vision/
├── cli.py           entrypoint (main)
├── __main__.py      python -m kiosk_vision
├── cameras/         reserved: ICameraSource and sources (KIO-10, KIO-11)
├── detection/       person detection (KIO-12), reserved for face detection (KIO-13)
│   ├── model.py     BoundingBox, PersonDetection, DetectionResult
│   ├── base.py      PersonDetector protocol, validate_frame
│   ├── onnx_detector.py OnnxPersonDetector (ONNX Runtime CPU)
│   └── stub_detector.py StubPersonDetector (testing and mocks)
├── tracking/        reserved: tracking, dwell time, zone of interest (KIO-14, KIO-15)
├── attributes/      reserved: age, gender, expression (KIO-16 to KIO-19)
└── events/          reserved: audience event publishing (KIO-20)
tests/
```

## Local development

Requires Python 3.11. From `services/vision-service`:

```bash
python3.11 -m venv .venv && . .venv/bin/activate
pip install -e ".[dev]"
ruff check .
ruff format --check .
pytest
```

Run a single test:

```bash
pytest tests/test_cli.py::test_main_prints_name_and_version
```

Run the service (prints the name and version and exits):

```bash
kiosk-vision
python -m kiosk_vision
```

## Docker

```bash
docker build -t kiosk-vision .
docker run --rm kiosk-vision
```

The image uses `python:3.11-slim` (amd64 and arm64) and runs as a non-root user.

## Docker Compose: two ways to run

From the repository root, after `cp .env.example .env`:

- **Everything on one host:** `docker compose --profile all-in-one up --build` starts PostgreSQL, the event bus (Valkey, Redis protocol), the backend, the web and `vision-service`.
- **Server and station on separate hosts:**
  - Server host: set `REDIS_BIND_ADDR` in `.env` to the server's LAN address (the default `127.0.0.1` only listens on that machine) and run `docker compose --profile server up --build`. The event bus refuses to start without `REDIS_PASSWORD`.
  - Station host: put `EVENT_TRANSPORT_URL=redis://:<password>@<server-host>:6379/0` in `.env` and run `docker compose --profile kiosk up --build`. Only `vision-service` starts there. Compose prints warnings about unset PostgreSQL variables; they are expected on a station and harmless. The player on the station is Chromium opening `http://<server>/player`.

`vision-service` has no `depends_on`, so it starts on a station that has no event bus or Postgres. Keep the event bus port (6379) on the store's local network and never expose it to the internet.

The skeleton does not read `EVENT_TRANSPORT_URL` yet (publishing arrives with KIO-20). The container prints its version and exits; it becomes a long-running process in later features.

## Detection (KIO-12)

The `kiosk_vision.detection` module provides CPU-based real-time person detection:
- `BoundingBox`: Normalized rectangular coordinates `[0.0, 1.0]` with converters to/from pixel coordinates.
- `PersonDetection`: Individual detection with confidence score.
- `DetectionResult`: Aggregated detections with count and latency timing in milliseconds.
- `OnnxPersonDetector`: Inference engine utilizing `onnxruntime` on CPU with SSDLite320-MobileNetV3 (exported from `ml/` via PyTorch).
- `StubPersonDetector`: In-memory detector for fast, deterministic unit testing without external model weights.

## Dependencies and Licensing

| Dependency | Purpose | License |
|---|---|---|
| `numpy` | Tensor manipulation and array operations | BSD-3-Clause |
| `opencv-python-headless` | Image preprocessing (resizing, color conversion) | Apache-2.0 |
| `onnxruntime` | CPU neural network inference | MIT |
| SSDLite320-MobileNetV3 | Pre-trained person detector architecture (torchvision) | BSD-3-Clause |

## Rules

- Never log or store frames, face crops, embeddings or per-person data. All frame processing is strictly in RAM.
- Do not add Ultralytics/YOLO or any AGPL or non-commercial dependency.
