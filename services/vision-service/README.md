# kiosk-vision-service

Vision service of the smart kiosk. It will capture camera frames, detect people and faces, track them, estimate age, gender and expression, and publish anonymous audience events. This version is only the skeleton (KIO-6): a package that installs, passes lint and tests, builds into an image and starts under Docker Compose. It has no runtime dependencies and does no camera, model or network work yet.

Design: [`spec/KIO-6-vision-skeleton/`](../../spec/KIO-6-vision-skeleton/) and [`docs/kiosko-arquitectura-mvp.md`](../../docs/kiosko-arquitectura-mvp.md).

## Layout

```
src/kiosk_vision/
├── cli.py           entrypoint (main)
├── __main__.py      python -m kiosk_vision
├── cameras/         reserved: ICameraSource and sources (KIO-10, KIO-11)
├── detection/       reserved: person and face detection (KIO-12, KIO-13)
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

- **Everything on one host:** `docker compose --profile all-in-one up --build` starts Postgres, Redis and `vision-service`.
- **Server and station on separate hosts:**
  - Server host: `docker compose --profile server up`.
  - Station host: set `EVENT_TRANSPORT_URL` in `.env` to the server's address and run `docker compose --profile kiosk up --build`. Only `vision-service` starts there.

`vision-service` has no `depends_on`, so it starts on a station that has no Redis or Postgres.

Known limitation: Redis is not published on a host port yet, so a separate station cannot reach the server's Redis. The skeleton does not read `EVENT_TRANSPORT_URL`, so nothing fails today. The event channel for remote stations is decided in KIO-9. The container prints its version and exits; it becomes a long-running process in later features.

## Rules

- Never log or store frames, face crops, embeddings or per-person data.
- Do not add Ultralytics/YOLO or any AGPL or non-commercial dependency.
