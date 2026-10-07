# KIO-6 Vision service skeleton: design

Implements `requirements.md` (R1–R10). Scope is the skeleton only: project definition, tooling, tests, package layout, entrypoint, container, compose service, CI activation and docs.

## 1. Overview

A minimal Python 3.11 package, `kiosk_vision`, installed from `services/vision-service/pyproject.toml` with `pip`. It has **no runtime dependencies**. A single console command (`kiosk-vision`, also `python -m kiosk_vision`) prints the service name and version and exits 0. The same package is built into a Docker image and declared in `docker-compose.yml` under the profiles `all-in-one` and `kiosk`, so it can start on the Ubuntu PC together with the server side, or alone on a separate station.

```
services/vision-service/
├── pyproject.toml
├── README.md
├── Dockerfile
├── .dockerignore
├── src/kiosk_vision/
│   ├── __init__.py          # __version__
│   ├── __main__.py          # python -m kiosk_vision
│   ├── cli.py               # main() -> int
│   ├── cameras/__init__.py      # reserved: ICameraSource and sources (KIO-10, KIO-11)
│   ├── detection/__init__.py    # reserved: person and face detection (KIO-12, KIO-13)
│   ├── tracking/__init__.py     # reserved: tracking and dwell time (KIO-14, KIO-15)
│   ├── attributes/__init__.py   # reserved: age, gender, expression (KIO-16..KIO-19)
│   └── events/__init__.py       # reserved: audience event publishing (KIO-20)
└── tests/
    └── test_cli.py
```

Files changed outside the service folder: `docker-compose.yml`, `.env.example`, `.github/workflows/ci.yml`, and `AGENTS.md` only if a documented command changes.

## 2. Decisions on the open questions

| Question | Decision | Why |
|---|---|---|
| Package name | Import name `kiosk_vision`; distribution name `kiosk-vision-service`; command `kiosk-vision` | Short, unambiguous, no clash with `cv` or `vision` on PyPI |
| Layout | `src/` layout | Tests always run against the installed package, which is what CI and Docker do |
| Build backend | `hatchling` | Small config, no `setup.py`, handles `src/` layout by default. `setuptools` would also work; no reason to prefer it |
| Ruff | `target-version = "py311"`, `line-length = 100`, rules `E`, `F`, `I`, `UP`, `B`; formatter default | Catches real bugs and import order without noisy style rules; can be widened later |
| Lock file | None for now | There are no runtime dependencies to pin. Revisit when KIO-12 adds ONNX Runtime and OpenCV |
| `EVENT_TRANSPORT_URL` default | `redis://:${REDIS_PASSWORD}@redis:6379/0` in compose, overridable through `.env` | Matches the existing Redis service, which requires a password |
| Compose layout | One `docker-compose.yml` with profiles | Already how the repo works. Revisit per-host override files in Phase 1 if needed |
| KIO-9 Trello card | Only the `vision-service` compose entry moves into KIO-6; KIO-9 keeps backend, player and wiring | Avoids duplicate work. Card text is edited only if the user asks |

## 3. Components

### 3.1 `pyproject.toml` (R1, R2)

- `[build-system]`: `requires = ["hatchling"]`, `build-backend = "hatchling.build"`.
- `[project]`: `name = "kiosk-vision-service"`, `version = "0.1.0"`, `requires-python = ">=3.11"`, `readme = "README.md"`, `dependencies = []`.
- `[project.optional-dependencies]`: `dev = ["pytest", "ruff"]`.
- `[project.scripts]`: `kiosk-vision = "kiosk_vision.cli:main"`.
- `[tool.hatch.build.targets.wheel]`: `packages = ["src/kiosk_vision"]`.
- `[tool.ruff]` and `[tool.ruff.lint]` as in section 2; `[tool.pytest.ini_options]`: `testpaths = ["tests"]`.

Version is declared once in `pyproject.toml` and read at runtime with `importlib.metadata.version("kiosk-vision-service")`, so there is no second copy to keep in sync.

### 3.2 Package and entrypoint (R4, R5)

- `kiosk_vision/__init__.py`: defines `__version__` from `importlib.metadata`, falling back to `"0+unknown"` if the package metadata is missing.
- `kiosk_vision/cli.py`: `main(argv: list[str] | None = None) -> int` prints `kiosk-vision-service <version>` and returns `0`. No argument parsing beyond what is needed; it ignores `argv` for now and is the place where later features add the run loop.
- `kiosk_vision/__main__.py`: `raise SystemExit(main())`.
- Reserved subpackages contain only a module docstring that names the owning feature and states what will live there. No imports, no code. This satisfies R4.2.
- The skeleton uses `print` for its single message and has no logging, no file writes and no network calls, which covers R5.3 and R10.1. The rule "never log frames or personal data" is written in the README for later features.

### 3.3 Tests (R3)

`tests/test_cli.py`:

- `test_version_is_exposed`: `kiosk_vision.__version__` is a non-empty string.
- `test_main_prints_name_and_version`: `main()` returns `0` and the captured output contains `kiosk-vision-service` and the version.
- `test_module_entrypoint`: `python -m kiosk_vision` run with `subprocess` and `sys.executable` exits `0` and prints the same line.
- `test_reserved_subpackages_import`: each reserved subpackage imports without side effects.

A failing test makes `pytest` exit non-zero, which fails the CI job (R3.3).

### 3.4 Container image (R6)

`Dockerfile`:

```dockerfile
FROM python:3.11-slim
ENV PYTHONDONTWRITEBYTECODE=1 PYTHONUNBUFFERED=1
WORKDIR /app
COPY pyproject.toml README.md ./
COPY src ./src
RUN pip install --no-cache-dir .
RUN useradd --system --no-create-home kiosk
USER kiosk
ENTRYPOINT ["kiosk-vision"]
```

- `python:3.11-slim` is an official multi-architecture image (amd64 and arm64), which supports R6.1. This feature verifies the amd64 build locally; an arm64 build is checked with `docker buildx build --platform linux/arm64` only if emulation is available, and is otherwise listed as not verified.
- `.dockerignore` excludes `.venv`, `__pycache__`, `.pytest_cache`, `.ruff_cache`, `tests`, `.env*` and `*.onnx`, so no secrets, datasets or weights enter the image (R6.5).
- The image does not copy `tests/` and installs without the `dev` extra.
- The container prints the version and exits 0 (R6.4). This is deliberate for the skeleton and is replaced by the long-running loop when real processing arrives.

### 3.5 Compose service (R7)

Added to `docker-compose.yml`:

```yaml
  vision-service:
    build: ./services/vision-service
    profiles: [all-in-one, kiosk]
    restart: "no"
    environment:
      EVENT_TRANSPORT_URL: ${EVENT_TRANSPORT_URL:-redis://:${REDIS_PASSWORD}@redis:6379/0}
```

- Profiles are `all-in-one` and `kiosk`; never `server` (R7.1).
- No `depends_on` (R7.4) and no `devices` entry (R7.6). The comment already in the file about `/dev/video0` stays as a pointer for KIO-10.
- `restart: "no"` because the skeleton exits immediately; a restart policy would loop. KIO-10 or later switches it to `unless-stopped` once the process is long-running.
- `.env.example` gets a commented line documenting the override for a separate station:

```
# On a separate kiosk host, point to the server side:
# EVENT_TRANSPORT_URL=redis://:<password>@<server-host>:6379/0
```

- Both modes:
  - All together: `docker compose --profile all-in-one up` starts Postgres, Redis and `vision-service`; the default URL resolves to the `redis` service.
  - Separate: server host runs `--profile server`; the station runs `--profile kiosk` with `EVENT_TRANSPORT_URL` set to the server's address. On the station, no other service is started.
- **Known limitation, not fixed here:** Redis is not published on a host port today, so a separate station cannot reach it yet. Publishing it, and deciding Redis versus HTTP as the transport, is part of KIO-9 and the open decision "event channel with remote stations" in the design doc. The skeleton does not use the URL, so nothing fails in the meantime. This is stated in the README.

### 3.6 CI changes (R8)

- `Vision service` job: no change in steps. It switches from skipped to active as soon as `pyproject.toml` exists (R8.1). Its pip cache already points to `services/vision-service/pyproject.toml`.
- `Docker Compose` job: the loop becomes `for profile in all-in-one server kiosk`, and the comment "The kiosk profile has no services yet" is removed (R8.2).
- A `docker build` step is **not** added to CI in this feature: it lengthens every PR and the build is verified locally. It is a candidate for a later job once the image has heavy dependencies (KIO-12).
- `CI success` already fails if any needed job fails (R8.3).

### 3.7 Documentation (R9)

`services/vision-service/README.md`: purpose, package layout (with the owning feature of each reserved folder), install, lint, test, run (`kiosk-vision`, `python -m kiosk_vision`), `docker build`, the two start modes and the limitation above, and the rule "no frames or personal data in logs or disk". `AGENTS.md` keeps its current CI commands because they already match; it is edited only if they change.

## 4. Dependencies and licenses (R1.4, R10.2)

| Package | Role | License |
|---|---|---|
| none | runtime | n/a |
| pytest | dev | MIT |
| ruff | dev | MIT |
| hatchling | build | MIT |
| python:3.11-slim | base image | Python Software Foundation license (image contains Debian packages under their own licenses) |

Confirmed (T11) from the installed package metadata: pytest 9.1.1, ruff 0.16.10 and hatchling 1.32.4 are MIT; their transitive dependencies `pluggy` and `iniconfig` are MIT and `packaging` is Apache-2.0 OR BSD-2-Clause. The base image license was not re-checked.

## 5. Testing and verification strategy

Local, in `services/vision-service`:

- `python -m venv .venv && . .venv/bin/activate && pip install -e ".[dev]"`
- `ruff check .` and `ruff format --check .`
- `pytest`
- `kiosk-vision` and `python -m kiosk_vision` print the line and exit 0
- `docker build -t kiosk-vision .` then `docker run --rm kiosk-vision` prints the line, exit 0; `docker run --rm kiosk-vision id -u` is not `0`

From the repo root:

- `cp .env.example .env && docker compose --profile all-in-one config -q && docker compose --profile kiosk config -q && docker compose --profile server config -q`
- `docker compose --profile kiosk up --build` on a machine with no server-side services runs and exits cleanly
- `docker compose --profile all-in-one up --build` starts Postgres, Redis and the vision service
- On GitHub, the PR shows `Vision service` actually running (not skipped), and `CI success` green

## 6. Requirement coverage

| Requirement | Covered by |
|---|---|
| R1 Python project | 3.1, section 4 |
| R2 Lint and format | 3.1 (ruff config), section 5 |
| R3 Tests | 3.3 |
| R4 Package layout | 1, 3.2, 3.7 |
| R5 Entrypoint | 3.1 (`scripts`), 3.2 |
| R6 Container image | 3.4 |
| R7 Compose and both modes | 3.5 |
| R8 CI activation | 3.6 |
| R9 Documentation | 3.7 |
| R10 Constraints | 3.2, 3.4, section 4 |

## 7. Risks and open points

- A container that exits immediately can look like a failure in `docker compose up`. Accepted for the skeleton and documented in the README.
- Separate-station mode cannot reach Redis until KIO-9 decides and exposes the channel (3.5).
- arm64 image build is not verified in this feature unless emulation is available.
- Hatchling is downloaded at build time, so building the image needs network access.
- Whether to edit the KIO-9 Trello card is left to the user.
