# KIO-9 Complete Docker Compose: tasks

Implements `design.md` for `requirements.md`. Do the tasks in order and tick each one when done. Commands run from the repository root unless a task says otherwise. Do not commit, push or open a PR until the user asks. If something in `design.md` turns out to be different when built, fix the spec first and tell the user. Remove the local `.env` and any container, volume or image created for a check when the task ends.

## Tasks

- [x] **T1 Web image: nginx template and Dockerfile** (R2.1–R2.7, R9.1)
  Create `apps/web/nginx/default.conf.template` as in `design.md` section 3.1 and delete `apps/web/nginx.conf`. In `apps/web/Dockerfile` replace the `nginx.conf` copy with the template copy and add the `ENV` defaults (`BACKEND_HOST`, `BACKEND_PORT`, `DNS_RESOLVER`, `CLIENT_MAX_BODY_SIZE`). Keep the health check and the non-root user.
  *Verify:* `docker build -t kiosko-web apps/web`; the `nginx -t` command from `design.md` section 3.3 succeeds with no backend running; `docker run --rm kiosko-web id` shows uid 101; `nginx -T` inside the image shows the substituted values and keeps `$uri`.

- [x] **T2 Compose and environment example** (R1.1–R1.3, R3.1–R3.2, R4.1, R4.2, R4.5, R5.2, R5.3, R9.1)
  In `docker-compose.yml`: add the `web` service; harden `redis` (`user: redis`, later `user: valkey`, the password wrapper, `ports` with `REDIS_BIND_ADDR`, health check with `REDISCLI_AUTH`, `restart`); remove `ports` from `core-backend`; update the comment at the top. Rewrite `.env.example` as in `design.md` section 3.2. `AGENTS.md` asks to ask before changing these files; the user already approved this scope.
  *Verify:* the file reads cleanly; `postgres` and `vision-service` are unchanged.

- [x] **T3 Static checks and the three profiles** (R1.4, R5.5, R6.2–R6.3)
  `cp .env.example .env`, then `docker compose --profile all-in-one|server|kiosk config -q`, and `config --services` for each: `kiosk` → `vision-service` only; `server` → `core-backend postgres redis web`; `all-in-one` → those four plus `vision-service`. Then simulate a station host: an `.env` containing only `EVENT_TRANSPORT_URL` and `docker compose --profile kiosk config -q` must pass without errors about server variables.
  *Verify:* all pass; record the service lists.

- [x] **T4 Server profile start and routing** (R1.2, R2.3–R2.5, R3.3–R3.4, R5.3, R6.2)
  `docker compose --profile server up -d --build --wait`. Check: all four services healthy; start order (PostgreSQL → backend → web) from the logs; `curl -i localhost/admin` and `/player` return 200 with the application; `/api/ping` returns the backend's 404 and not `index.html`; `/actuator/health` through the web returns 404; `localhost:8080` refuses the connection; the three security headers are present; `docker compose ps` shows no host port for `core-backend`.
  *Verify:* record each observation in the "Verification results" section.

- [x] **T5 Redis security** (R4.1–R4.5, R5.1)
  With the server profile running: from a throwaway client (`REDIS_BIND_ADDR=0.0.0.0`, `docker run --rm --add-host=host.docker.internal:host-gateway redis:7 redis-cli -h host.docker.internal ...`) the right password returns `PONG`, a wrong one and no password are rejected; with the default bind address the host port is on `127.0.0.1` only (`docker compose port redis 6379` or `docker ps`); the health check turns healthy. Then start with `REDIS_PASSWORD=` empty: the `redis` container exits with `REDIS_PASSWORD is required`. Confirm `docker compose exec redis id` is not root.
  *Verify:* record the outputs.

- [x] **T6 Resilience of the proxy** (R2.6)
  With the stack running, recreate only the backend (`docker compose up -d --force-recreate core-backend`) and confirm that `localhost/api/ping` keeps returning the backend's response after it is healthy again, without restarting `web`.
  *Verify:* record the result; if nginx keeps the old address, fix the template before continuing.

- [x] **T7 `all-in-one`, `kiosk` and clean-up** (R6.1, R6.3, R6.4)
  `docker compose --profile server down -v`. Then `--profile all-in-one up --build -d`: PostgreSQL, Redis, `core-backend` and `web` healthy and `vision-service` exited with status 0; `down -v`. Then `--profile kiosk up --build` on its own: only `vision-service`, exit 0; `down -v`. Confirm `docker ps -a`, volumes and networks of the project are gone.
  *Verify:* record the states.

- [x] **T8 CI** (R7.1–R7.5)
  In `.github/workflows/ci.yml`: extend the `compose` job (profile content checks and the `nginx -t` step) and add the `compose-smoke` job as in `design.md` section 3.3, and add it to `needs` of `ci-success`. Validate the YAML. Run the smoke script locally exactly as the job would, including the `REDIS_BIND_ADDR=0.0.0.0` line.
  *Verify:* YAML parses; the local run of the script passes; after the PR is opened, `Docker Compose`, `compose-smoke` and `CI success` are green.

- [x] **T9 READMEs and `AGENTS.md`** (R8.1–R8.5, R8.8)
  Update, from the repository facts and as listed in `design.md` section 3.4: the root `README.md` (Spanish), `apps/web/README.md` (English), `services/core-backend/README.md`, `services/vision-service/README.md` and `AGENTS.md`. Add a note to `spec/KIO-7-backend-skeleton/requirements.md` that R9.3 (publishing the backend port) is replaced by KIO-9 R3.2.
  *Verify:* every command written in these files is run or labeled as not run.

- [x] **T10 Architecture document, revision 3** (R8.6, R8.8)
  Update `docs/kiosko-arquitectura-mvp.md` as listed in `design.md` section 3.4: changelog, stack table, physical architecture text and both diagrams, backend Clean Architecture and database independence section, repository structure, decisions and risks tables (event channel decided, TLS accepted risk), implementation status table, rewritten next steps, pointer to the administration document. Only decisions already agreed; do not invent new ones.
  *Verify:* diff the document against the repository (ports, profiles, paths, versions from `pom.xml` and `package.json`); list anything labeled "planned".

- [x] **T11 Consistency pass** (R8.7)
  Grep the documentation for stale statements and fix them: "fase de diseño", "solo Postgres y Redis", "8080:8080", "no se publica Redis", "SPA servida por el backend", "no hay código". Re-run the commands of the READMEs that were not run in T9.
  *Verify:* the grep returns nothing stale; the list of commands run and not run is recorded.

- [x] **T12 Image licenses and privacy review** (R9.1–R9.4)
  Review the license of every image the compose file uses (`postgres:16`, `redis:7`, `nginxinc/nginx-unprivileged:alpine`, `eclipse-temurin`, `python:3.11-slim`) from the image's own license information. **Check in particular `redis:7`:** the image pulled during design reports Redis 7.4.11, and to my knowledge Redis moved from BSD to source-available licenses starting with 7.4, which may not fit R9.4 or the project rule against restrictive licenses; if confirmed, propose a permissive alternative (the last BSD release, or a BSD-licensed fork) and ask the user before changing it. Also confirm that no volume stores frames or per-person data and that no secret is baked into an image.
  *Verify:* a license table in `design.md` section 4 (new), with confirmed and unconfirmed entries; the Redis finding reported to the user.

- [x] **T13 Final pass**
  `git status` shows only intended files (the spec, compose, `.env.example`, `.github/workflows/ci.yml`, `apps/web` changes, READMEs, `AGENTS.md`, the architecture document, the KIO-7 spec note); no `.env`, volumes or build output are tracked; tick every task; re-read `requirements.md` and confirm each requirement has a covering task and a recorded result; report what was verified and what was not. Wait for the user to ask for the commit, push and PR (`feature/KIO-9-compose-complete` → `develop`, reviewed by the KIO-8 author).

## Optional, only if the user asks

- Rewrite the Trello card KIO-9 text to the real scope and move it to "En review" when the PR opens.
- Add Docker layer caching to the `compose-smoke` job if it is too slow.

## Requirement coverage

| Requirement | Tasks |
|---|---|
| R1 Web service | T2, T3, T4 |
| R2 Reverse proxy | T1, T4, T6 |
| R3 Single entry point | T2, T3, T4 |
| R4 Redis for stations | T2, T5 |
| R5 Fail-fast and order | T2, T3, T4, T5 |
| R6 End to end | T3, T4, T7 |
| R7 CI | T8 |
| R8 Documentation | T9, T10, T11 |
| R9 Constraints | T1, T2, T5, T12 |

## Verification results (2026-10-08)

- **T1 Web image:** `nginx -t` passes with the template mounted and no backend; the built image runs as uid 101; the rendered config substitutes only the four variables and keeps `$uri` and `$backend`; env overrides work (`BACKEND_HOST`, `BACKEND_PORT`). Started alone, the image serves `/admin` and `/player` (200) and answers `502` on `/api` (no backend) without refusing to start.
- **T2/T3 Compose:** `config -q` passes for the three profiles; `kiosk` lists only `vision-service`, `server` lists `core-backend postgres redis web`, `all-in-one` adds `vision-service`. A station `.env` with only `EVENT_TRANSPORT_URL` validates with exit 0; Compose prints harmless "variable is not set" warnings for the PostgreSQL variables of inactive profiles (R5.5 reworded accordingly).
- **T4 Server profile:** postgres → backend → web started in that order and all four are healthy. `/admin` and `/player` return 200 with the application; `/api/ping` returns the backend's JSON 404 (not `index.html`); `/actuator/health` through the web returns 404; `localhost:8080` refuses the connection; the three security headers are present; the only published ports are `127.0.0.1:6379` (Redis) and `80` (web).
- **Finding in T4:** the KIO-8 `HEALTHCHECK` used `localhost`, which resolves to `::1` first while nginx listens on IPv4 only, so the web container was always `unhealthy` under Compose. Fixed by using `127.0.0.1`.
- **T5 Redis:** default bind is loopback only; with `REDIS_BIND_ADDR=0.0.0.0` a throwaway client container through the host gateway gets `PONG` with the right password, `WRONGPASS` with a wrong one and `NOAUTH` without one; the container runs as uid 999 (not root); with `REDIS_PASSWORD=` empty the container exits with `REDIS_PASSWORD is required` (restarting under `unless-stopped`) and the health check is healthy with a password.
- **T6 Proxy resilience:** the backend IP was forced to change (`172.18.0.4` → `172.18.0.6`) and the same `web` container kept proxying. Finding: with the backend down, nginx waited for the default 30 s resolver timeout; `resolver_timeout 5s` was added and the answer is now `502` in 0.19 s while `/admin` still returns 200.
- **T7 Profiles end to end:** `all-in-one` reached postgres, redis, backend and web healthy with `vision-service` exited 0 and `/admin` 200 on port 80; `kiosk` with only `EVENT_TRANSPORT_URL` started `vision-service` alone and exited 0; `down -v` left no containers, volumes or networks.
- **T8 CI:** YAML valid. The steps of the `compose` job and of the new `compose-smoke` job were run locally exactly as GitHub runs them (`bash -eo pipefail`) and both pass. The first local run caught a bug in the Redis check (`grep -q` closing a pipe under `pipefail`); fixed by capturing the output first. The job uses port 8081 to avoid clashes on the runner.
- **T9/T11 Documentation:** the root README, `apps/web`, `core-backend` and `vision-service` READMEs and `AGENTS.md` were updated. Commands run and confirmed: `docker compose --profile all-in-one up --build -d` / `down -v`, `/admin` and `/player` on port 80, `docker compose exec core-backend curl .../actuator/health`, the `nginx -t` command from `apps/web`, and `docker run -p 8080:8080 kiosko-web`. The stale-phrase grep found nothing. **Not run:** `./mvnw spring-boot:run` outside Compose (unchanged from KIO-7), the station start on a real second host.
- **T10 Architecture document:** revision 3 written: changelog, stack table, physical architecture and both diagrams, backend architecture section, repository structure, decisions made, open risks, implementation status and next steps. Versions and ports were checked against `pom.xml`, `package.json`, `docker-compose.yml` and the READMEs.
- **T12 Licenses:** see `design.md` section 8. `redis:7` resolved to Redis 7.4.11, which is RSALv2/SSPLv1 (source-available, not OSI). **The user decided to switch to Valkey (see the Valkey follow-up below).** Other image licenses are listed there, with the ones taken from knowledge marked. `apps/web/.dockerignore` now excludes `.env*`.
- **Not verified:** the GitHub run of the CI (it happens when the PR is opened), the station behavior on a second physical host, WebSocket through the proxy (no backend `/ws` until KIO-29), the arm64 build of the web image, and the licenses marked "from knowledge".

## Follow-up: switch from Redis to Valkey (2026-10-08, user decision)

- **Change:** `redis:7` → `valkey/valkey:8` (Valkey 8.1.10, BSD-3-Clause confirmed from the upstream `COPYING`), `user: valkey`, `valkey-server` in the wrapper, `valkey-cli` in the health check and in the CI client. The service name `redis`, the `REDIS_*` variables and the `redis://` URLs are unchanged.
- **Re-verified on Valkey:** the `compose` and `compose-smoke` CI jobs run locally exactly as GitHub runs them and pass (routes, proxy, unpublished 8080, `PONG` / `WRONGPASS` / `NOAUTH`); the server profile is healthy; the container runs as uid 999; default bind is `127.0.0.1:6379`; an empty `REDIS_PASSWORD` makes the bus refuse to start with `REDIS_PASSWORD is required`; `all-in-one` reaches healthy with `vision-service` exited 0 and `/admin` 200; no containers or volumes were left behind.
- **Also verified on the image:** an empty `--requirepass` disables authentication on Valkey too, so the wrapper is still required; `REDISCLI_AUTH` works with `valkey-cli`; the image ships `redis-cli` as a symlink.
- **Documentation updated:** root README, architecture document (stack row, physical architecture, diagrams, decisions table), `AGENTS.md` (with the rule not to switch back to Redis), the vision and backend READMEs, `.env.example`, `requirements.md` (decision 5 and R9.4) and `design.md`.
- **Not verified:** a Redis client library against Valkey (no client code exists yet, KIO-20 and KIO-23), and the `valkey/valkey:8` tag over time (it floats within major 8).
