# KIO-6 Vision service skeleton: requirements

## Introduction

`services/vision-service` is the Python service that will capture camera frames, detect people and faces, track them, estimate attributes and publish audience events (see `docs/kiosko-arquitectura-mvp.md`). This feature creates only its skeleton: a buildable, testable, containerized Python project that starts under Docker Compose in both deployment modes, with no camera, model or network logic yet.

Goals:

- Give the following features (KIO-10 camera sources, KIO-12 detection, KIO-20 events, KIO-21 health) a stable place to land.
- Turn on the `Vision service` job in CI (`.github/workflows/ci.yml`), which today skips because `pyproject.toml` does not exist.
- Prove from the first version that the project runs in the two supported topologies: everything on one host (`all-in-one`), or the server side (admin, backend, Postgres, Redis) on one host and the station (camera and screen) on another (`server` and `kiosk` profiles).

Source: Trello card KIO-6 and `docs/kiosko-arquitectura-mvp.md`.

## Requirements

### R1 Python project definition

**User story:** As a developer, I want a standard Python project definition so that the service installs reproducibly on any machine and in CI.

Acceptance criteria:

- R1.1 The service shall be defined by `services/vision-service/pyproject.toml` and shall require Python 3.11.
- R1.2 When a developer runs `pip install -e ".[dev]"` in `services/vision-service`, the system shall install the package and a `dev` extra containing `ruff` and `pytest`.
- R1.3 The package shall be importable by its name after installation.
- R1.4 The runtime dependencies shall be limited to what the skeleton needs and shall use permissive licenses; the system shall not depend on Ultralytics/YOLO or any AGPL or non-commercial component.

### R2 Lint and format

**User story:** As a developer, I want automatic lint and format checks so that code style is consistent and enforced by CI.

Acceptance criteria:

- R2.1 The `ruff` configuration shall live in `pyproject.toml`.
- R2.2 When `ruff check .` runs in `services/vision-service`, the system shall report no violations.
- R2.3 When `ruff format --check .` runs in `services/vision-service`, the system shall report no formatting changes needed.

### R3 Tests

**User story:** As a developer, I want a working test setup so that every later feature can add tests from day one.

Acceptance criteria:

- R3.1 The service shall have a tests folder discovered by `pytest` without extra arguments.
- R3.2 When `pytest` runs in `services/vision-service`, the system shall pass at least one test that exercises real behavior of the package (for example import, version and entrypoint).
- R3.3 If a test fails, then `pytest` shall exit with a non-zero status so that CI fails.

### R4 Package layout

**User story:** As a developer, I want the package structure reserved for the planned components so that later features add code without reorganizing the project.

Acceptance criteria:

- R4.1 The package shall use a documented layout that reserves places for camera sources, detection, tracking, attributes and event publishing.
- R4.2 The reserved places shall contain no functional logic in this feature: no camera access, no model loading and no network calls.
- R4.3 The layout shall be described in the service README.

### R5 Entrypoint

**User story:** As an operator, I want a command that starts the service so that the container has something to run and I can confirm the installation.

Acceptance criteria:

- R5.1 When the entrypoint is invoked, the system shall print the service name and version and exit with status 0.
- R5.2 The entrypoint shall be callable both as an installed command and as `python -m <package>`.
- R5.3 While the service runs, it shall not write frames, images or personal data to logs or disk.

### R6 Container image

**User story:** As an operator, I want a container image so that the service runs identically on the Ubuntu PC and, later, on a Raspberry Pi.

Acceptance criteria:

- R6.1 The service shall have a `Dockerfile` based on a Python 3.11 slim image that is published for both amd64 and arm64.
- R6.2 When `docker build` runs on `services/vision-service`, the system shall build an image that installs the package.
- R6.3 The container shall run as a non-root user.
- R6.4 When the container starts, it shall run the entrypoint and exit with status 0.
- R6.5 The image shall not contain secrets, `.env` files, datasets or model weights.

### R7 Compose service and the two deployment modes

**User story:** As an operator, I want the project to start with Docker Compose in either topology so that I can run everything on one server or split the server side from the station.

Acceptance criteria:

- R7.1 The compose file shall declare `vision-service` under the profiles `all-in-one` and `kiosk`, and shall not declare it under `server`.
- R7.2 When `docker compose --profile all-in-one up` runs, the system shall build and start `vision-service` together with the existing server-side services.
- R7.3 When `docker compose --profile kiosk up` runs on a host that has none of the server-side services, the system shall build and start `vision-service` without errors.
- R7.4 The `vision-service` definition shall not use `depends_on` on Redis or Postgres, because those services do not exist on a separate station host.
- R7.5 The address of the server side shall be configured through an environment variable `EVENT_TRANSPORT_URL`, declared in `.env.example`, with a default that targets the `redis` service for `all-in-one` and that can be overridden to the server's address for `kiosk`. The skeleton shall not read or use it yet.
- R7.6 The `vision-service` definition shall not map a camera device in this feature (KIO-10 adds it), so that `up` works on any machine, including macOS.
- R7.7 When `docker compose --profile kiosk config -q` and `docker compose --profile all-in-one config -q` run with `.env` copied from `.env.example`, the system shall report no errors.

### R8 CI activation

**User story:** As a team member, I want CI to check the service on every pull request so that regressions are caught before merging into `develop`.

Acceptance criteria:

- R8.1 When `services/vision-service/pyproject.toml` exists, the `Vision service` job shall run install, `ruff check`, `ruff format --check` and `pytest`, and shall pass, without changing those steps of the workflow.
- R8.2 The `Docker Compose` job shall also validate the `kiosk` profile, and the workflow note that says the `kiosk` profile has no services yet shall be removed.
- R8.3 If any of these checks fails, then the `CI success` check shall fail.

### R9 Developer documentation

**User story:** As a developer, I want short instructions in the service folder so that I can install, test and run the service without asking.

Acceptance criteria:

- R9.1 `services/vision-service/README.md` shall document install, lint, test, run, `docker build`, and the package layout.
- R9.2 The README shall document both start modes: `docker compose --profile all-in-one up`, and `docker compose --profile kiosk up` on the station with `EVENT_TRANSPORT_URL` set to the server's address.
- R9.3 If any command listed in the CI section of `AGENTS.md` changes, then `AGENTS.md` shall be updated in the same pull request.

### R10 Constraints carried over

**User story:** As the project owner, I want the privacy and licensing rules to hold from the first line of code.

Acceptance criteria:

- R10.1 The system shall not store frames, face crops, embeddings or per-person data in this feature.
- R10.2 The dependency list shall be reviewed for license compatibility before the pull request is opened.

## Out of scope

- Camera access and the `/dev/video0` device mapping (KIO-10, KIO-11).
- Models, ONNX Runtime and detection (KIO-12 and later).
- Event publishing and any Redis client code (KIO-20).
- Health endpoint and metrics (KIO-21).
- The backend, Postgres and Redis wiring, and the player service (KIO-7, KIO-9, KIO-35). KIO-9 keeps the remaining compose work; only the `vision-service` entry moves into this feature.

## Open questions (to settle in design.md)

1. Package name and `src` versus flat layout.
2. Build backend (`hatchling` or `setuptools`).
3. Ruff rule set and line length.
4. Whether to use a dependency lock file.
5. Default value of `EVENT_TRANSPORT_URL` for `all-in-one`.
6. Whether to split compose into a base file plus per-host overrides later.
7. Whether KIO-9's Trello card should be edited to reflect that the `vision-service` compose entry moved here.
