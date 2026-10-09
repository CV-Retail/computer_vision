# KIO-9 Complete Docker Compose: design

Implements `requirements.md` (R1–R9). Scope: the `web` service and its reverse proxy, a single published entry point, Redis published with a password for separate stations, fail-fast configuration, CI guards (including a Compose smoke job), and the update of the architecture document and every README.

## 1. Overview

```
 browser (laptop, tablet, or Chromium on the station)
        │  http://<server>:80
        ▼
 ┌─ server side (profiles all-in-one and server) ───────────────────────────────┐
 │  web (nginx :8080 inside)                                                    │
 │   ├─ /admin  /player  /…   → single-page application (history fallback)      │
 │   ├─ /api/                 → core-backend:8080                               │
 │   ├─ /ws                   → core-backend:8080  (WebSocket upgrade)          │
 │   └─ /actuator/            → 404 (never exposed)                             │
 │  core-backend (internal only) ──► postgres:5432                              │
 │  redis :6379 (password; published on loopback by default)                    │
 └──────────────────────────────────────────────────────────────────────────────┘
        ▲ audience events (redis://:<password>@<server>:6379)
 ┌─ station (profile kiosk) ───────────────┐
 │  vision-service        Chromium → http://<server>/player
 └─────────────────────────────────────────┘
```

Published host ports, before and after:

| Service | Before | After |
|---|---|---|
| `core-backend` | `8080` | none (internal) |
| `web` | not in Compose | `${WEB_PORT:-80}` → container `8080` |
| `redis` | none | `${REDIS_BIND_ADDR:-127.0.0.1}:6379` |
| `postgres` | none | none |

Files changed:

| File | Change |
|---|---|
| `docker-compose.yml` | `web` service; `redis` hardened and published; `core-backend` unpublished; comments |
| `.env.example` | every variable documented; `WEB_PORT`, `REDIS_BIND_ADDR` added |
| `apps/web/nginx.conf` → `apps/web/nginx/default.conf.template` | becomes a template with the proxy |
| `apps/web/Dockerfile` | copies the template, defines the default environment |
| `.github/workflows/ci.yml` | `compose` job extended; new `compose-smoke` job; `ci-success` needs it |
| `README.md`, `apps/web/README.md`, `services/core-backend/README.md`, `services/vision-service/README.md` | updated (R8) |
| `AGENTS.md` | updated (R8.5) |
| `docs/kiosko-arquitectura-mvp.md` | new revision (R8.6) |
| `spec/KIO-7-backend-skeleton/requirements.md` | note that R9.3 (publish the backend port) is replaced by KIO-9 |

## 2. Decisions on the open questions

| Question | Decision | Evidence or reason |
|---|---|---|
| nginx upstream configuration | nginx image **templates**: `apps/web/nginx/default.conf.template` rendered at start with `envsubst` from `BACKEND_HOST`, `BACKEND_PORT`, `DNS_RESOLVER`, `CLIENT_MAX_BODY_SIZE`, with defaults set by `ENV` in the Dockerfile | Verified on `nginxinc/nginx-unprivileged:alpine` (nginx 1.31.6): only the declared variables are substituted and `$uri`, `$backend` and the others are preserved |
| Upstream resolution | `set $backend "http://${BACKEND_HOST}:${BACKEND_PORT}"; proxy_pass $backend;` plus `resolver ${DNS_RESOLVER} valid=10s` (default `127.0.0.11`, Docker's DNS) | The backend can be recreated alone without restarting nginx (a static `proxy_pass` keeps the old IP), and `nginx -t` passes even when the backend does not exist (verified), which makes the CI check trivial. The image's built-in `NGINX_LOCAL_RESOLVERS` was not usable in templates (verified), hence an explicit variable |
| Upload limits | `client_max_body_size 100m`, `proxy_read_timeout 300s` for `/api/`, `3600s` for `/ws` | **Provisional.** KIO-31 and KIO-61 decide the real limits; the values are variables or one-line edits |
| CI smoke job | **Yes**, a `compose-smoke` job on the `server` profile | Four services now depend on each other; `config -q` cannot catch a broken proxy, a wrong health check or an unpublished port. Cost: a few minutes per pull request |
| Web port | host variable `WEB_PORT` (default `80`); the container always listens on `8080` | A development machine that already uses port 80 sets `WEB_PORT=8081` |
| TLS | **Not in the MVP.** Accepted and documented limitation: the admin login travels in clear text on the store's local network | Recorded as an accepted risk in the architecture document; to be revisited before the pilot |
| Fail-fast configuration | **No `${VAR:?}`.** Each service fails by itself when its variable is missing | Experiment: Compose interpolates `:?` even for services in inactive profiles, so it would break a station host that has no `REDIS_PASSWORD` (requirement R5.5). PostgreSQL's image and the backend (`Could not resolve placeholder 'DB_URL'`) already fail naming the problem; Redis gets a wrapper (next row) |
| Event bus image | **`valkey/valkey:8`** (Valkey 8.1.10, BSD-3-Clause) instead of `redis:7` | Decided after the license review (section 8): `redis:7` resolves to 7.4.x, RSALv2/SSPLv1. Valkey is protocol-compatible; the service name `redis`, the `REDIS_*` variables and the `redis://` URLs are kept because they are the protocol's |
| Redis password | The `redis` command is a small `sh -c` wrapper that exits with `REDIS_PASSWORD is required` when the variable is empty, then `exec valkey-server --requirepass "$REDIS_PASSWORD"`; `user: valkey` | Experiment: `--requirepass ""` **disables authentication** (a client got `PONG` without a password), on Redis and on Valkey. Experiment: a `sh -c` command runs as root (uid 0) unless `user: valkey` is set (the image only drops privileges when the command is `valkey-server` directly), which would break R9.1 |
| Redis health check | `valkey-cli ping` with the password in `REDISCLI_AUTH` | Verified on Valkey: right password → `PONG`; wrong → `WRONGPASS`; no warning that a password was passed on the command line. The image also ships `redis-cli` as a symlink |
| Redis exposure | `ports: "${REDIS_BIND_ADDR:-127.0.0.1}:6379:6379"`; a server host sets `REDIS_BIND_ADDR` to its LAN address | Safe default on a development machine and on `all-in-one`; explicit opt-in to listen on the network |
| Security headers | `X-Content-Type-Options: nosniff`, `X-Frame-Options: SAMEORIGIN`, `Referrer-Policy: same-origin` | Cheap hardening of the admin portal; no content security policy yet (needs the real UI) |
| Coordination with KIO-8 | The change touches the KIO-8 `nginx.conf` and `Dockerfile`; the pull request reviewer is the KIO-8 author and the PR description says so | Both files belong to that feature, but KIO-8 is merged |
| Trello card KIO-9 | Rewrite its text to the real scope after approval | The original text is already satisfied by KIO-6 and KIO-7 |

## 3. Components

### 3.1 Web image and proxy (R1, R2, R3)

`apps/web/nginx/default.conf.template` (replaces `apps/web/nginx.conf`):

```nginx
server {
    listen 8080;
    server_name localhost;
    root /usr/share/nginx/html;
    index index.html;

    resolver ${DNS_RESOLVER} valid=10s;
    resolver_timeout 5s;
    client_max_body_size ${CLIENT_MAX_BODY_SIZE};

    add_header X-Content-Type-Options "nosniff" always;
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header Referrer-Policy "same-origin" always;

    location /api/ {
        set $backend "http://${BACKEND_HOST}:${BACKEND_PORT}";
        proxy_pass $backend;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_read_timeout 300s;
        proxy_send_timeout 300s;
    }

    location /ws {
        set $backend "http://${BACKEND_HOST}:${BACKEND_PORT}";
        proxy_pass $backend;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_read_timeout 3600s;
    }

    location /actuator/ { return 404; }

    location / {
        try_files $uri $uri/ /index.html;
    }
}
```

- `resolver_timeout 5s` (added after T6): with the backend down, nginx waited for the default 30 s resolver timeout before answering.
- `proxy_pass $backend;` has no URI part, so `/api/...` reaches the backend with the same path. Backend errors pass through unchanged (`proxy_intercept_errors` stays off), so an unknown `/api/x` returns the backend's 404 and never `index.html` (R2.5, R3.4).
- `add_header` inside a `location` would drop the `server`-level headers; the headers are defined once at `server` level and no `location` defines its own `add_header`.

`apps/web/Dockerfile` changes (runtime stage only):

```dockerfile
FROM nginxinc/nginx-unprivileged:alpine
ENV BACKEND_HOST=core-backend \
    BACKEND_PORT=8080 \
    DNS_RESOLVER=127.0.0.11 \
    CLIENT_MAX_BODY_SIZE=100m
COPY nginx/default.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 8080
HEALTHCHECK ... wget -qO- http://127.0.0.1:8080/   (was localhost: it resolves to ::1 first and nginx listens on IPv4 only, so the KIO-8 health check could never pass; found when the image was first run under Compose)
```

The image keeps running as `nginx` (uid 101, non-root; verified). The template mechanism writes `/etc/nginx/conf.d/default.conf` at start, which works with the unprivileged user (verified by `nginx -t` and `nginx -T`).

### 3.2 Compose (R1, R3, R4, R5, R6)

Target `docker-compose.yml` (changed parts; `postgres` and `vision-service` stay as they are):

```yaml
  redis:                      # Valkey: speaks the Redis protocol
    image: valkey/valkey:8
    profiles: [all-in-one, server]
    restart: unless-stopped
    user: valkey
    environment:
      REDIS_PASSWORD: ${REDIS_PASSWORD:-}
      REDISCLI_AUTH: ${REDIS_PASSWORD:-}
    command:
      - sh
      - -c
      - 'test -n "$$REDIS_PASSWORD" || { echo "REDIS_PASSWORD is required" >&2; exit 1; }; exec valkey-server --requirepass "$$REDIS_PASSWORD"'
    ports:
      - "${REDIS_BIND_ADDR:-127.0.0.1}:6379:6379"
    healthcheck:
      test: ["CMD-SHELL", "valkey-cli ping | grep -q PONG"]
      interval: 5s
      timeout: 3s
      retries: 10

  core-backend:
    ...                       # unchanged, except: no `ports:`
    # Internal only: reached through the web proxy (/api, /ws).

  web:
    build: ./apps/web
    profiles: [all-in-one, server]
    restart: unless-stopped
    depends_on:
      core-backend:
        condition: service_healthy
    ports:
      - "${WEB_PORT:-80}:8080"
```

- Start order: PostgreSQL healthy → `core-backend` healthy → `web`. Redis has a health check and is not a dependency of the backend yet (R5.3).
- `${VAR:-}` (empty default) is used for Redis so that Compose never prints "variable is not set" and the failure comes from the service with a clear message.
- The top comment of the file documents the profiles and the single entry point.

`.env.example` (target):

```
# --- Server side (profiles all-in-one and server) ---
POSTGRES_USER=kiosko
POSTGRES_PASSWORD=change-me
POSTGRES_DB=kiosko
# Required. Redis refuses to start without it.
REDIS_PASSWORD=change-me
# Host port of the web portal (admin and player). Use another one if 80 is taken.
WEB_PORT=80
# Address Redis listens on at the host. 127.0.0.1 = this machine only;
# set the server's LAN address (or 0.0.0.0) so stations can reach it. Never expose it to the internet.
REDIS_BIND_ADDR=127.0.0.1

# --- Separate station (profile kiosk) ---
# Point vision to the server's Redis. If unset, vision uses the redis service of this same compose.
# EVENT_TRANSPORT_URL=redis://:<password>@<server-host>:6379/0
```

### 3.3 CI (R7)

`compose` job (extended):

1. `cp .env.example .env`; `docker compose --profile <p> config -q` for `all-in-one`, `server`, `kiosk` (unchanged).
2. Profile content: `kiosk` lists exactly `vision-service`; `server` and `all-in-one` list `web` and `core-backend`; `server` does not list `vision-service` (R1.1, R1.4, R7.2). Implemented with `docker compose --profile <p> config --services`.
3. nginx syntax:

```bash
docker run --rm \
  -v "$PWD/apps/web/nginx/default.conf.template:/etc/nginx/templates/default.conf.template:ro" \
  -e BACKEND_HOST=core-backend -e BACKEND_PORT=8080 \
  -e DNS_RESOLVER=127.0.0.11 -e CLIENT_MAX_BODY_SIZE=100m \
  nginxinc/nginx-unprivileged:alpine nginx -t
```

New `compose-smoke` job (needed by `ci-success`), timeout 20 minutes:

1. `cp .env.example .env`; append `REDIS_BIND_ADDR=0.0.0.0` (so a container can reach Redis through the host gateway, simulating a remote station).
2. `docker compose --profile server up -d --build --wait --wait-timeout 300`. `--wait` fails the job if a service is unhealthy.
3. Checks (R7.5):
   - `GET /admin` and `/player` return 200 and contain the application's root element;
   - `GET /api/ping` returns 404 and the body does not contain the root element (proves the route reaches the backend);
   - `GET /actuator/health` through the web returns 404;
   - port 8080 on the host refuses the connection (backend not published);
   - `docker run --rm --add-host=host.docker.internal:host-gateway valkey/valkey:8 valkey-cli -h host.docker.internal -a "$REDIS_PASSWORD" ping` returns `PONG`, and with a wrong password returns `WRONGPASS`.
4. On failure, print `docker compose logs`; always run `docker compose down -v`.

The existing `all-in-one` path (including `vision-service` exiting with status 0) is checked locally, not in CI: `--wait` treats an exited container as a failure.

### 3.4 Documentation update (R8)

Method: write each change from the repository, then run every command and read every port, profile and path back from the files (R8.7). Anything not built stays labeled "planned".

| Document | Changes |
|---|---|
| `README.md` (es) | Replace "fase de diseño" and "solo Postgres y Redis" with the real status; "Cómo levantarlo" for the three modes (`all-in-one`; `server` + `kiosk` with `EVENT_TRANSPORT_URL`), the variables, `/admin` and `/player` on port 80, the Redis exposure warning; updated stack table; `spec/` and `.github/` in the structure; spec-driven workflow, PR rules and Trello cards; open decisions updated; pointer to the specs and to the admin document |
| `apps/web/README.md` (en) | Docker section: started by Compose; where the proxy lives (`nginx/default.conf.template`); the four variables and their defaults; how to run `nginx -t` locally |
| `services/core-backend/README.md` (en) | Compose section: internal only; reach `/api` and `/ws` through the web; check health from inside with `docker compose exec`; the local `curl localhost:8080` stays valid when running with `./mvnw spring-boot:run` outside Compose |
| `services/vision-service/README.md` (en) | Remove the Redis limitation; station start with the password and `REDIS_BIND_ADDR` on the server |
| `AGENTS.md` (en) | Status line; architecture bullets (web is a nginx container, not served by the backend); Compose commands and ports; `/api` and `/ws` rule; CI section (new job) |
| `docs/kiosko-arquitectura-mvp.md` (es) | Revision 3, see below |

Architecture document, revision 3 (Spanish; only decisions already agreed):

- **Changelog** of the revision: web served by an nginx container with a reverse proxy and a single entry point; Redis on the local network with a password; backend with Clean Architecture, DDD and database independence; spec-driven workflow, gitflow and CI; campaign administration document; state of implementation.
- **Stack table:** backend Java 21, Spring Boot 4.1 and Modulith 2.1 with Flyway and Maven Wrapper (tests on H2); web React 18, Vite and TypeScript served by nginx; Redis with password; the row "SPA servida por el backend" is corrected.
- **Physical architecture and diagrams:** both diagrams redrawn as in section 1 of this design, in Spanish, with `web`, port 80 and the player URL on the station.
- **Backend section (new):** layers `domain`, `application`, `infrastructure` per module, the dependency rule enforced by tests, published surface, database independence by vendor folders.
- **Repository structure:** adds `spec/`, `.github/`, `AGENTS.md` and `docker-compose.yml` details.
- **Decisions:** the row "Canal de eventos con estaciones remotas" moves from open risks to decided ("Redis en la LAN con contraseña; HTTP/MQTT descartado por ahora"); a new accepted risk "Sin TLS en la LAN" and the open point about the mandatory vetoes remain.
- **State of implementation (new section):** table by feature (KIO-1, 6, 7, 8, 9, 47 done or in progress; the rest planned) and a rewritten "Próximos pasos" that no longer lists finished items.
- **Pointer** to the campaign administration document in `docs/`.

## 4. Verification strategy

Local, from the repository root with `.env` copied from `.env.example`:

- `docker compose --profile all-in-one|server|kiosk config -q` pass; `kiosk` lists only `vision-service`.
- `docker compose --profile server up -d --build --wait`: PostgreSQL, Redis, `core-backend` and `web` healthy.
- `curl -i localhost/admin`, `/player` (200, application), `/api/ping` (404 from the backend, not `index.html`), `/actuator/health` (404), `localhost:8080` refused.
- Redis from a throwaway client container (`REDIS_BIND_ADDR=0.0.0.0`): right password → `PONG`; wrong password and no password → rejected.
- `REDIS_PASSWORD=` empty: the `redis` container exits with `REDIS_PASSWORD is required` and `docker compose ps` shows it; the other services still report their own errors.
- Station host simulation: `.env` containing only `EVENT_TRANSPORT_URL`, `docker compose --profile kiosk up --build` works without errors about server variables (R5.5).
- Recreate only the backend (`docker compose up -d --force-recreate core-backend`) and confirm the web keeps proxying without a restart.
- `docker compose --profile all-in-one up --build -d`: everything healthy and `vision-service` exited 0; `down -v` leaves nothing.
- Images run as non-root: `docker compose exec web id`, `exec redis id` (uid 999), backend and vision as before.
- Documentation: every command in the READMEs and in `AGENTS.md` is run or labeled as not run; the architecture document is diffed against the repository facts.
- On GitHub: `Docker Compose`, `compose-smoke` and `CI success` are green.

## 5. Requirement coverage

| Requirement | Covered by |
|---|---|
| R1 Web service | 3.2, 3.3 |
| R2 Reverse proxy | 3.1, section 2 |
| R3 Single entry point | 1, 3.2, 3.3 |
| R4 Redis for stations | 3.2, section 2, 3.3 (smoke), 4 |
| R5 Fail-fast and order | 3.2, section 2 (deviation explained), 4 |
| R6 End to end | 4 |
| R7 CI | 3.3 |
| R8 Documentation | 3.4 |
| R9 Constraints | 3.1 (non-root nginx), 3.2 (`user: valkey`), 3.2 (`.env.example` placeholders) |

## 6. Differences from the first version of the requirements

- R4.2 and R5.1 were reworded: the guard is in each service, not in Compose, because Compose interpolates the variables of inactive profiles. The requirements file already reflects this.
- R5.5 and R7.5 were added; R8 was broadened to include the architecture document and every README.

## 7. Risks and open points

- **Redis is a network service.** Mitigations: loopback by default, mandatory password enforced by the container, documentation. A weak password such as `change-me` is still possible: the README says to change it and the smoke job uses the example value only inside CI.
- **No TLS:** the admin password crosses the store's LAN in clear text. Accepted for the MVP; revisit before the pilot.
- **Port 80** can clash on a development machine; `WEB_PORT` solves it.
- **The smoke job lengthens each pull request** (image builds without cache). Layer caching can be added later.
- **`up --wait` cannot cover `all-in-one`** because `vision-service` exits by design; that path is verified locally.
- **WebSocket proxying is configured but untested end to end**: the backend has no `/ws` endpoint until KIO-29. Only the configuration syntax and the upgrade headers are verified.
- **The proxy limits (100 MB, timeouts)** are provisional until KIO-31 and KIO-61.
- **KIO-8 files are modified** by another feature's pull request; the KIO-8 author reviews it.

## 8. Image licenses and privacy review (task T12, 2026-10-08)

| Image | Shipped in | License | Status |
|---|---|---|---|
| `postgres:16` (PostgreSQL 16.15) | server | PostgreSQL License (permissive) | From knowledge; the image has no license label |
| `nginxinc/nginx-unprivileged:alpine` (nginx 1.31.6) | web | nginx: BSD-2-Clause; the image's own label says Apache-2.0 | Label confirmed; nginx license from knowledge |
| `eclipse-temurin:21-jre` | backend | GPL-2.0 with the Classpath Exception | From knowledge; no license label |
| `python:3.11-slim` | vision | Python Software Foundation License | From knowledge; no license label |
| `node:20-alpine`, `eclipse-temurin:21-jdk` | build stages only, not in the final images | MIT, GPL-2.0 with the Classpath Exception | Not shipped |
| `redis:7` (Redis **7.4.11**) | **replaced, no longer used** | RSALv2 or SSPLv1 (source-available, not OSI) | Finding of the review: the `redis:7` tag resolves to 7.4.x, and Redis moved from BSD-3-Clause to RSALv2/SSPLv1 with 7.4 (March 2024). The user decided to replace it |
| **`valkey/valkey:8`** (Valkey **8.1.10**) | server (and the CI smoke client) | **BSD-3-Clause** | Confirmed from the upstream repository's `COPYING` (BSD 3-Clause, Valkey contributors, with Redis Ltd. copyright up to 2020). The image's own labels carry only the source URL |

Finding on Redis, and its resolution: the project rule is permissive licenses only, and R9.4 asks for licenses compatible with a closed commercial product. RSALv2 and SSPLv1 are not OSI licenses and restrict offering Redis as a service; bundling it inside a product shipped to customers is the case that normally needs a license review. **Resolution (2026-10-08, user decision): switch the event bus to Valkey.** Verified on Valkey 8.1.10: the `sh -c` wrapper works with `user: valkey` (uid 999), an empty password refuses to start, an empty `--requirepass` also disables authentication (so the wrapper stays), `REDISCLI_AUTH` works with `valkey-cli`, and the Compose, local CI and all-in-one checks of tasks T4 to T8 pass again. The service name `redis`, the `REDIS_*` variables and the `redis://` URLs are unchanged. Redis clients (the future vision publisher and the backend consumer) are protocol-compatible with Valkey. The `valkey/valkey:8` tag floats within major version 8; the team may pin a full version later.

Privacy and secrets: the only named volume is `pgdata` (database); no volume stores frames or per-person data. No image copies `.env` (the backend and vision `.dockerignore` already exclude it; `apps/web/.dockerignore` now does too, since `COPY . .` copies the build context into the Node stage). `.env.example` contains only placeholders.
