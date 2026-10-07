# AGENTS.md

Shared instructions for every AI coding agent working in this repository (Claude Code, Gemini CLI, others). This file is the single source of truth: `CLAUDE.md` imports it and `.gemini/settings.json` points Gemini CLI to it. Edit rules here, never in a tool-specific file.

## Project status

No application code yet, no build, no tests. The design lives in [docs/kiosko-arquitectura-mvp.md](docs/kiosko-arquitectura-mvp.md) (Spanish, revision 2) and is the source of truth: read it before creating anything and update it when a decision changes. When code lands, add the build, lint and test commands here (including how to run a single test).

## What this is

A kiosk with a camera and a screen that picks the advertising based on who is standing in front of it (number of people, age range, gender, expression, dwell time) without storing or identifying anyone. The customer is the store, which creates its own campaigns. The MVP is one store, one Ubuntu PC (CPU only), everything local, no cloud.

## Architecture (planned monorepo)

- `services/vision-service` (Python 3.11, OpenCV, ONNX Runtime): capture, detection, tracking and attributes. Publishes audience events; it does not know the backend.
- `services/core-backend` (Java 21, Spring Boot, Spring Modulith with modules Campaigns, Rules, Reports; Postgres + Flyway): domain and rules engine. Sends playback orders to the player over WebSocket.
- `apps/web` (React, Vite, TypeScript): one app with `/admin` and `/player` routes (Chromium in kiosk mode), built and served as static files by the backend.
- `ml/` (PyTorch): training and evaluation; only exports versioned `.onnx` models. Do not mix with the production service.
- `contracts/`: versioned JSON schemas (`audience-event`, `campaign`). Python and Java validate against the same files; neither depends on the other's code.
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
- **Java 21:** respect Spring Modulith boundaries (Campaigns, Rules, Reports); no access to another module's internals. Schema changes only through Flyway migrations; never edit an applied migration.
- **TypeScript:** strict mode, a single app serving `/admin` and `/player`.
- Keep schema field names as defined in `contracts/` (they are Spanish: `edad`, `genero`, `expresion`, `confianza`, `motivo`).

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
