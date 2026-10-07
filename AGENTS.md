# AGENTS.md

Shared instructions for every AI coding agent working in this repository (Claude Code, Gemini CLI, others). This file is the single source of truth: `CLAUDE.md` imports it and `.gemini/settings.json` points Gemini CLI to it. Edit rules here, never in a tool-specific file.

## Project status

Early stage: the `services/vision-service` (KIO-6) and `services/core-backend` (KIO-7) skeletons exist; web and ML are not started. The design lives in [docs/kiosko-arquitectura-mvp.md](docs/kiosko-arquitectura-mvp.md) (Spanish, revision 2) and is the source of truth: read it before creating anything and update it when a decision changes. When code lands, add the build, lint and test commands here (including how to run a single test).

## What this is

A kiosk with a camera and a screen that picks the advertising based on who is standing in front of it (number of people, age range, gender, expression, dwell time) without storing or identifying anyone. The customer is the store, which creates its own campaigns. The MVP is one store, one Ubuntu PC (CPU only), everything local, no cloud.

## Architecture (planned monorepo)

- `services/vision-service` (Python 3.11, OpenCV, ONNX Runtime): capture, detection, tracking and attributes. Publishes audience events; it does not know the backend.
- `services/core-backend` (Java 21, Spring Boot, Spring Modulith with modules Campaigns, Rules, Reports; Postgres + Flyway): domain and rules engine. Sends playback orders to the player over WebSocket.
- `apps/web` (React, Vite, TypeScript): one app with `/admin` and `/player` routes (Chromium in kiosk mode), built and served as static files by the backend.
- `ml/` (PyTorch): training and evaluation; only exports versioned `.onnx` models. Do not mix with the production service.
- `contracts/`: versioned JSON schemas (`audience-event`, `campaign`). Python and Java validate against the same files; neither depends on the other's code.
- `spec/`: one folder per feature with its requirements, design and tasks (see "Spec-driven workflow").
- Docker Compose profiles: `all-in-one` (everything on one host) and `server` + `kiosk` (separate stations, including Raspberry Pi / ARM64).

Flow: camera → vision → event on Redis (pub/sub) → Java adapter turns it into a domain event → rules engine → player.

Cameras go through `ICameraSource` (USB/V4L2, CSI, RTSP), chosen by configuration. On Ubuntu pass `/dev/video0` to the container; on macOS Docker cannot see the webcam, so run vision directly on the host.

## Domain rules

- **Privacy:** frames are discarded; tracking lives only in memory; only aggregates per time window are stored, never one row per person. The audience event carries no bounding boxes and no tracking ids.
- **Licensing:** do not use YOLO/Ultralytics (AGPL) or pretrained SCRFD/InsightFace weights (non-commercial). Check the license of every weight and dataset; research-only ones (UTKFace, AffectNet, RAF-DB) are not valid for the product.
- **Deterministic engine, no AI in decisions:** fixed order: schedule/scope → vetoes → audience over the group → priority/weight → default playlist. A veto always wins.
- **Confidence:** every attribute carries `confianza` and is only used by rules above the threshold; otherwise it is "unknown". For vetoes an unknown age counts as a possible minor (alcohol: veto if anyone looks under 25). Expression is off by default.
- **Age in ranges**, not years. Babies and children are evaluated separately because models fail more there.
- **One group** is evaluated, from the camera's zone of interest. Each campaign has a minimum on-screen time to avoid flicker.
- **Audience events** (`motivo`): `cambio_composicion`, `latido` (about every 2 s) and `cierre_dwell`.
- **Imported campaigns** (JSON/ZIP) are untrusted input: validate against the schema and always create them as drafts.
- `storeId` and `kioskId` go on every record from the start.

## Code conventions

These are proposals; confirm them with the team when the first code is written.

- **Language:** identifiers, comments, commit messages and this file are in English. User-facing UI strings may be Spanish. `README.md` and `docs/` stay in Spanish.
- **Python 3.11:** type hints everywhere, `ruff` for lint and format, `pytest` for tests.
- **Java 21:** respect Spring Modulith boundaries (Campaigns, Rules, Reports); no access to another module's internals. Schema changes only through Flyway migrations; never edit an applied migration. Unit tests are named `*Test` (no Spring context, database or network; run with `./mvnw test`); integration tests are named `*IT` (may start Spring and H2; run by `./mvnw verify`). Test domain and application logic with unit tests; keep integration tests few.
- **TypeScript:** strict mode, a single app serving `/admin` and `/player`.
- Keep schema field names as defined in `contracts/` (they are Spanish: `edad`, `genero`, `expresion`, `confianza`, `motivo`).

## Backend architecture (core-backend)

The backend follows Clean Architecture with DDD inside each Spring Modulith module (`campaigns`, `rules`, `reports`), with the packages `domain`, `application` and `infrastructure` (spec: `spec/KIO-7-backend-skeleton/`). Domain code starts in KIO-24.

- **Dependency rule:** `domain` depends on nothing but the Java standard library. `application` depends on `domain` (and on the published surface of other modules). `infrastructure` depends on both. Nothing depends on `infrastructure` except the Spring wiring. Modules talk only through their published surface, which is the module's base package (`domain`, `application` and `infrastructure` are internal). These rules are enforced by tests and break the build.
- **Domain:** aggregates, entities, value objects, domain events and domain services; no Spring, JPA, Jakarta Persistence, Flyway, Jackson or JDBC.
- **Application:** use cases; repositories are declared as interfaces (ports) in `domain` or `application`.
- **Infrastructure:** adapters (database, Redis, HTTP, WebSocket) implement the ports. Keep all vendor-specific code here.
- **Database independence:** the domain never knows the database engine. Flyway loads `db/migration/common` (portable SQL) plus `db/migration/<vendor>`; supporting a new engine means a JDBC driver, a vendor migration folder and configuration, with no change in `domain` or `application`. Tests run on H2, which does not prove compatibility with other engines.

## Git workflow (gitflow)

- Work in `feature/<TASK>-<description>` or `bugfix/<TASK>-<description>` (for example `feature/KIO-12-rules-engine`; the `KIO` task-code prefix is an assumption to confirm). Open a PR into `develop`.
- `develop` must stay stable: CI runs the full suite on every PR and on every push to it.
- Promotion is a manual PR `develop` → `main`, merged only after `develop` is green. `release/*` and `hotfix/*` are also allowed (PRs into `main` and back into `develop`).
- Never push directly to `develop` or `main`.
- Commit messages in English.

When a PR is opened, `.github/workflows/pr-automation.yml` requests review from every member except the author and labels it from the branch name (`feature/` → `enhancement`, `bugfix/` → `bug`, `hotfix/` → `bug` + `hotfix`, `release/` or `develop` → `main` → `release`). The member list is in that file; update it when someone joins or leaves.

### Opening a pull request

Never push or open a PR unless the user asks. When asked, write the body from `.github/pull_request_template.md` to a scratch file and open the PR with `gh`:

```bash
gh pr create --base develop --title "KIO-N <short description>" --body-file <file>
```

- The title is `KIO-N <short description>`, with the same words as the branch description and spaces instead of hyphens. Example: branch `feature/KIO-6-vision-skeleton` → title `KIO-6 vision skeleton`. The base is `develop`; only promotion PRs go from `develop` to `main`.
- `## Links` comes first: the Trello card as a markdown link whose text is the card title exactly as in Trello, for example `Trello card: [KIO-6 Esqueleto de vision-service](https://trello.com/c/lCrz4NLw/11-kio-6-esqueleto-de-vision-service)` (read title and URL from the Trello connector; if it is not available, ask the user), and the spec folder `spec/<branch name without feature/ or bugfix/>/`.
- `## Description` comes second: what changed and why, taken from the diff and the spec, and what was tested and what was not.

## Spec-driven workflow

Every feature is built from three documents in `spec/<TASK>-<description>/`, for example `spec/KIO-6-vision-skeleton/`. The folder name is the branch name without the `feature/` or `bugfix/` prefix. The Trello card with the same `KIO-N` code is the source of the title and the first acceptance criteria.

1. `requirements.md`: user stories with EARS acceptance criteria. Ids are `R1`, `R2`, … and sub-criteria `R1.1`. Include an introduction, an out-of-scope list and open questions. EARS forms: "When <event>, the system shall <response>", "While <state>, …", "If <condition>, then …", "The system shall …".
2. `design.md`: overview, structure and architecture, interfaces and data, decisions and trade-offs, testing strategy, and a table mapping every requirement id to the part of the design that covers it.
3. `tasks.md`: an ordered checklist (`- [ ] T1 …`), each task small, referencing requirement ids and ending with how to verify it. Tick tasks as they are done.

Rules:

- Order and gates: requirements → approval → design → approval → tasks → approval → implementation. Stop after each document and wait for the user's approval. Never write code before `tasks.md` is approved.
- Specs are written in English and committed in the same branch and PR as the code. Do not add `spec/` to `.gitignore`.
- If the design or code diverges from a requirement, update the spec first and tell the user.
- Implement tasks in order, one at a time, and keep the task list current.

## CI

`.github/workflows/ci.yml` runs on PRs and pushes to `develop` and `main`. Run the same checks locally before pushing:

- Contracts: `check-jsonschema --check-metaschema contracts/*.schema.json`
- Compose: `cp .env.example .env && docker compose --profile all-in-one config -q` (also `server` and `kiosk`)
- Vision (`services/vision-service`): `pip install -e ".[dev]"`, then `ruff check .`, `ruff format --check .`, `pytest` (single test: `pytest tests/test_cli.py::test_main_prints_name_and_version`)
- Backend (`services/core-backend`): `./mvnw -B -ntp verify` (unit tests only: `./mvnw test`; one unit test: `./mvnw -Dtest=ModularityTest test`; one integration test: `./mvnw verify -Dit.test=HealthEndpointIT -Dtest=NoMatch -Dsurefire.failIfNoSpecifiedTests=false`)
- Web (`apps/web`): `npm ci`, then the `lint`, `typecheck`, `test` and `build` scripts

Component jobs skip until their project file exists (`pyproject.toml`, `pom.xml`, `package.json`). The single required check is `CI success`.

## Agent boundaries

Ask the user before:

- Changing anything in `contracts/*.schema.json`. Contract changes are versioned as a new `vN` file and must be applied on both the Python and the Java side.
- Changing `docker-compose.yml`, `.env.example`, license decisions or the veto policy.
- Changing a decision recorded in `docs/kiosko-arquitectura-mvp.md`.

Never:

- Commit `.env`, datasets, model weights or media.
- Store frames, face crops, embeddings or per-person rows.
- Add out-of-scope items: cloud, multi-store, licensing server, fleet monitoring, visual template editor, retail media, person identification, external signage integration, Kubernetes.
- Invent commands, versions, licenses or benchmark numbers. Verify them, or label them as unverified.

When two agents may touch the same area, keep changes small and focused so a human can review them, and say in your summary which files you changed.
