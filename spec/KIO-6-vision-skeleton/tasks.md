# KIO-6 Vision service skeleton: tasks

Implements `design.md` for `requirements.md`. Do the tasks in order and tick each one when done. Commands run from `services/vision-service` unless a task says "repo root". Do not commit, push or open a PR until the user asks.

## Tasks

- [x] **T1 Project definition** (R1.1, R1.2, R1.4, R2.1)
  Create `services/vision-service/pyproject.toml` with the hatchling build system, project metadata (`kiosk-vision-service`, `0.1.0`, Python `>=3.11`, no runtime dependencies), the `dev` extra (`pytest`, `ruff`), the `kiosk-vision` script, the wheel `packages` setting, and the `ruff` and `pytest` configuration from `design.md` section 3.1. Add a minimal `README.md` so `readme = "README.md"` resolves (full content comes in T10).
  *Verify:* the file parses (`python -c "import tomllib; tomllib.load(open('pyproject.toml','rb'))"`).

- [x] **T2 Package and entrypoint** (R1.3, R4.1–R4.2, R5.1–R5.3)
  Create `src/kiosk_vision/` with `__init__.py` (`__version__` from `importlib.metadata`, fallback `"0+unknown"`), `cli.py` (`main()` prints `kiosk-vision-service <version>` and returns `0`), `__main__.py`, and the five reserved subpackages (`cameras`, `detection`, `tracking`, `attributes`, `events`), each with only a docstring that names the owning feature. No camera, model, network, logging or file writes.
  *Verify:* `python -c "import ast,sys; [ast.parse(open(f).read()) for f in sys.argv[1:]]" $(find src -name '*.py')` shows no syntax errors.

- [x] **T3 Tests** (R3.1–R3.3)
  Create `tests/test_cli.py` with the four tests from `design.md` section 3.3: version exposed, `main()` output and return code, `python -m kiosk_vision` through `subprocess`, reserved subpackages import.
  *Verify:* done together with T4.

- [x] **T4 Install and run the local checks** (R1.2, R2.2, R2.3, R3.2, R5.1, R5.2)
  Create a virtualenv (`python3.11 -m venv .venv`, already ignored by git), run `pip install -e ".[dev]"`, then `ruff check .`, `ruff format --check .` and `pytest`. Run `kiosk-vision` and `python -m kiosk_vision`. If `ruff format --check` reports changes, run `ruff format .` and re-check. If Python 3.11 is not available, say so and use the available version only for a smoke check, labeled as such.
  *Verify:* all commands succeed; both entrypoints print the line and exit 0.

- [x] **T5 Dockerfile and `.dockerignore`** (R6.1, R6.3, R6.5)
  Create `Dockerfile` as in `design.md` section 3.4 and `.dockerignore` excluding virtualenvs, caches, `tests`, `.env*`, `*.onnx`, `*.pt`, `*.pth` and `media`.
  *Verify:* `docker build --check .` if the installed Docker supports it; otherwise review by eye against the design.

- [x] **T6 Build and run the image** (R6.2, R6.4, R6.3)
  Requires a running Docker daemon. `docker build -t kiosk-vision .`, `docker run --rm kiosk-vision` (prints the line, exit 0), `docker run --rm --entrypoint id kiosk-vision -u` (not `0`). Try `docker buildx build --platform linux/arm64 .` only if emulation is available; otherwise report arm64 as not verified.
  *Verify:* outputs as described; report anything not run.

- [x] **T7 Compose service and environment example** (R7.1, R7.4–R7.6; repo root)
  Add `vision-service` to `docker-compose.yml` as in `design.md` section 3.5 (profiles `all-in-one` and `kiosk`, `restart: "no"`, `EVENT_TRANSPORT_URL` with its default, no `depends_on`, no `devices`), and update the profile comments at the top and the placeholder comment. Add the commented `EVENT_TRANSPORT_URL` override example to `.env.example`. The user already approved touching these two files for this feature.
  *Verify:* file reads cleanly; no changes to the `postgres` and `redis` services.

- [x] **T8 Verify both deployment modes locally** (R7.2, R7.3, R7.7; repo root)
  `cp .env.example .env`, then `docker compose --profile all-in-one config -q`, `--profile server config -q` and `--profile kiosk config -q`. With the Docker daemon running: `docker compose --profile kiosk up --build` (only `vision-service` starts, prints the line, exits 0), then `docker compose --profile all-in-one up --build` (Postgres, Redis and `vision-service`), and stop everything with `docker compose --profile all-in-one down`. Delete the local `.env` afterwards (it is git-ignored, but do not leave credentials around).
  *Verify:* all commands succeed; note that the separate-station Redis reachability is a known, documented limitation.

- [x] **T9 CI workflow** (R8.1–R8.3)
  In `.github/workflows/ci.yml`, change the compose loop to `all-in-one server kiosk` and remove the comment that says the `kiosk` profile has no services. Leave the `Vision service` job steps unchanged. Validate the YAML with `python -c "import yaml; yaml.safe_load(open('.github/workflows/ci.yml'))"`.
  *Verify:* YAML parses; after the PR is opened, `Vision service` runs (not skipped) and `CI success` is green.

- [x] **T10 Documentation** (R9.1–R9.3, R4.3, R10.1)
  Write the full `services/vision-service/README.md`: purpose, layout with the owning feature of each reserved folder, install, lint, test, run, `docker build`, the two start modes with their commands, the known Redis-reachability limitation for separate stations, and the rule that frames and personal data are never logged or stored. Check that the commands in the `CI` section of `AGENTS.md` still match; edit only if they differ.
  *Verify:* every command in the README was run in T4 to T8, or is labeled as not run.

- [x] **T11 Dependency and privacy review** (R1.4, R10.1, R10.2)
  List the final dependencies (runtime: none; dev: `pytest`, `ruff`; build: `hatchling`) and confirm their licenses online, replacing the from-memory table in `design.md` if anything differs. Grep the package for any `open(`, `write`, `logging`, `socket`, `requests` or `cv2` usage (expected: none).
  *Verify:* license table confirmed or corrected; grep returns nothing.

- [x] **T12 Final pass**
  From the repo root: `git status` shows only the intended files (spec documents, `AGENTS.md`, `services/vision-service/**`, `docker-compose.yml`, `.env.example`, `.github/workflows/ci.yml`); no `.env`, caches or virtualenv are tracked; re-run T4's checks once; tick every task here and re-read `requirements.md` to confirm each requirement has a covering task. Report to the user what was verified and what was not (for example arm64). Wait for the user to ask for the commit, push and PR (`feature/KIO-6-vision-skeleton` → `develop`).

## Optional, only if the user asks

- Move Trello card KIO-6 to "En proceso" now, and to "En review" when the PR opens.
- Edit the KIO-9 card to note that the `vision-service` compose entry moved into KIO-6.

## Requirement coverage

| Requirement | Tasks |
|---|---|
| R1 | T1, T2, T4, T11 |
| R2 | T1, T4 |
| R3 | T3, T4 |
| R4 | T2, T10 |
| R5 | T2, T4 |
| R6 | T5, T6 |
| R7 | T7, T8 |
| R8 | T9 |
| R9 | T10 |
| R10 | T2, T10, T11 |

## Verification results (2026-10-07)

- Local: Python 3.11.16, `ruff check` and `ruff format --check` clean, `pytest` 8 passed, `kiosk-vision` and `python -m kiosk_vision` print `kiosk-vision-service 0.1.0` and exit 0.
- Image: `docker build --check` no warnings; build succeeds natively on linux/arm64 (Apple Silicon host) and also for linux/amd64 under emulation, where it runs and prints the version; runs as uid 999 (non-root); `/app` has no tests, `.env` or weights.
- Compose: `config -q` passes for `all-in-one`, `server` and `kiosk`; `kiosk` resolves to `vision-service` only and `server` to none of it. `--profile kiosk up --build` starts only `vision-service` (prints the line, exit 0). `--profile all-in-one up --build` starts Postgres, Redis and `vision-service`. With `EVENT_TRANSPORT_URL` overridden and no `REDIS_PASSWORD`, `config` shows the override without warnings.
- Licenses: pytest, ruff, hatchling, pluggy and iniconfig are MIT; packaging is Apache-2.0 OR BSD-2-Clause.
- Not verified: the CI run on GitHub (it happens when the PR is opened), the arm64 image on a real Raspberry Pi, and reachability of a remote Redis from a separate station (known limitation, deferred to KIO-9).
