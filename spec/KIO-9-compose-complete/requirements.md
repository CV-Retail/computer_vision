# KIO-9 Complete Docker Compose: requirements

## Introduction

KIO-6 and KIO-7 already put `vision-service` and `core-backend` in `docker-compose.yml` under the three profiles `all-in-one`, `server` and `kiosk`, and the CI validates the three profiles with `docker compose config`. What is missing to call the Compose setup complete, now that the web application exists (KIO-8), is:

- the **web application** as a service (nginx serving `/admin` and `/player`), reachable from a browser through **one entry point**;
- the **reverse proxy** that connects the browser to the backend without CORS;
- the **event channel** that lets a separate station reach the server (Redis published with a password);
- fail-fast configuration, health checks and start order for the whole server side;
- documentation of the two ways to run the project.

The topologies from `docs/kiosko-arquitectura-mvp.md` stay the same:

- **All together (`all-in-one`)**: one host runs everything, including the Chromium player.
- **Separate**: the server host runs the `server` profile (PostgreSQL, Redis, backend, web); each station runs the `kiosk` profile (vision) plus Chromium, which opens `http://<server>/player`.

Decisions taken for this feature (user answers on 2026-10-08):

1. The `web` service runs **only on the server side** (`all-in-one` and `server`), never under `kiosk`. A station needs no copy of the web.
2. The browser reaches the API through an **nginx reverse proxy** (`/api` and `/ws`), same origin, so there is no CORS.
3. Only the **web** is published on the host, on **port 80**. The backend becomes internal. This replaces the `8080:8080` publication of KIO-7 (requirement R9.3 of that spec).
4. **The event bus is published with a password** so that a separate station can send audience events. This closes the open decision "event channel with remote stations" of the architecture document.
5. **The event bus runs on Valkey, not Redis** (decided on 2026-10-08, after the license review in task T12). It speaks the Redis protocol, so the service name `redis`, the variables `REDIS_*` and the `redis://` URLs stay. Redis 7.4 and later are RSALv2/SSPLv1 (source-available, not OSI), which the project does not accept; Valkey is BSD-3-Clause and is the fork of the last BSD line (Redis 7.2).

Source: Trello card KIO-9, `docs/kiosko-arquitectura-mvp.md`, and the specs of KIO-6, KIO-7 and KIO-8.

## Requirements

### R1 Web service in Compose

**User story:** As an operator, I want the web application started by Compose so that the admin portal and the player are available without extra steps.

Acceptance criteria:

- R1.1 The compose file shall declare a `web` service built from `apps/web`, under the profiles `all-in-one` and `server`, and shall not declare it under `kiosk`.
- R1.2 When `docker compose --profile all-in-one up` or `--profile server up` runs, the system shall start `web` only after `core-backend` reports healthy.
- R1.3 The `web` service shall use the health check of its image and `restart: unless-stopped`.
- R1.4 When `docker compose --profile kiosk config --services` runs, the system shall list only `vision-service`.

### R2 Reverse proxy to the backend

**User story:** As a user of the admin portal, I want the browser to talk to the backend through the same address as the web so that nothing needs CORS and only one port is exposed.

Acceptance criteria:

- R2.1 The nginx of the `web` image shall forward requests under `/api/` to `core-backend` inside the Compose network.
- R2.2 The nginx shall forward WebSocket connections under `/ws` to `core-backend`, with the upgrade headers.
- R2.3 The routes `/admin`, `/player` and every other non-API path shall keep serving the single-page application, with the history fallback to `index.html`.
- R2.4 The nginx shall not forward `/actuator/*` (the backend health endpoint stays internal).
- R2.5 If the backend answers an `/api/` request with an error, then the proxy shall return that error unchanged (it shall not fall back to `index.html`).
- R2.6 The backend host name used by the proxy shall be configurable, with the default `core-backend`, so the image is not tied to Compose service names.
- R2.7 The proxy shall define a request body limit and timeouts suitable for media uploads, as documented values, until KIO-31 and KIO-61 fix the real limits.

### R3 Single published entry point

**User story:** As an operator, I want one port for the whole portal so that I reach it from a laptop or a tablet and expose as little as possible.

Acceptance criteria:

- R3.1 The only application port published on the host by the `all-in-one` and `server` profiles shall be the web port, defaulting to 80 and configurable through an environment variable declared in `.env.example`.
- R3.2 The `core-backend` service shall no longer publish a host port (the backend stays reachable only inside the Compose network).
- R3.3 When a browser opens `http://<host>/admin` and `http://<host>/player`, the system shall serve the application.
- R3.4 When a client requests `http://<host>/api/` for a path the backend does not define, the system shall return the backend's error response, which proves the proxy route works.

### R4 Event channel for separate stations

**User story:** As an operator of a separate station, I want to reach the server's event bus so that vision can publish audience events to it.

Acceptance criteria:

- R4.1 The `redis` service shall publish port 6379 on the host, bound by default to the loopback address, with the bind address configurable through an environment variable so a server host can bind it to its LAN address.
- R4.2 Redis shall always require a password. If `REDIS_PASSWORD` is empty or not set, then the `redis` service shall refuse to start with an error that names the variable. An empty `--requirepass` would silently disable authentication, so it shall never reach Redis.
- R4.3 When `EVENT_TRANSPORT_URL` on a station points to the server's Redis, a client on the station host shall be able to authenticate and receive a reply to `PING`.
- R4.4 A client without the password, or with a wrong one, shall be rejected.
- R4.5 The `redis` service shall have a health check that authenticates with the password.
- R4.6 The documentation shall state that the Redis port must stay on the store's local network and never be exposed to the internet.

### R5 Fail-fast configuration and start order

**User story:** As an operator, I want a clear error when the configuration is incomplete so that I do not discover it later at runtime.

Acceptance criteria:

- R5.1 If `POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_DB` or `REDIS_PASSWORD` is missing, then the service that needs it shall fail to start with an error that names the variable. Compose-level required-variable checks (`${VAR:?}`) shall not be used, because Compose interpolates the variables of inactive profiles and would break a station host.
- R5.2 `.env.example` shall declare every variable the compose file uses, each with a short comment, including the web port and the Redis bind address.
- R5.3 The start order shall be: PostgreSQL healthy → `core-backend` healthy → `web`. Redis has its own health check and is not a dependency of the backend yet, because no backend code uses it.
- R5.4 The `.env` file shall not be tracked by git.
- R5.5 A station host shall be able to run `docker compose --profile kiosk` with an `.env` that defines only the variables it needs (`EVENT_TRANSPORT_URL`), without errors. Compose prints harmless warnings ("variable is not set") for the PostgreSQL variables of the inactive profiles; the station README shall say they are expected.

### R6 End-to-end start of the whole project

**User story:** As a team member, I want to start everything with two commands so that I can check the whole system quickly.

Acceptance criteria:

- R6.1 After `cp .env.example .env`, when `docker compose --profile all-in-one up --build -d` runs on a clean machine, the system shall reach a state in which PostgreSQL, Redis, `core-backend` and `web` are healthy, and `vision-service` has run and exited with status 0 (its skeleton behavior).
- R6.2 When `docker compose --profile server up --build -d` runs, the system shall start the same server-side services and not `vision-service`.
- R6.3 When `docker compose --profile kiosk up --build` runs on a host without the server-side services, the system shall start only `vision-service` without errors.
- R6.4 `docker compose down -v` shall remove everything the three profiles created.

### R7 CI

**User story:** As a team member, I want CI to guard the Compose setup so that a broken compose file or proxy configuration does not reach `develop`.

Acceptance criteria:

- R7.1 The `Docker Compose` job shall keep validating the three profiles with `docker compose config -q`.
- R7.2 The job shall also check that `kiosk` contains only `vision-service` and that `web` and `core-backend` are absent from `kiosk`.
- R7.3 The nginx configuration shall be syntax-checked in CI.
- R7.4 If any of these checks fails, then the `CI success` check shall fail.
- R7.5 The CI shall include a job that starts the `server` profile with `docker compose up --wait` and verifies that `/admin` and `/player` serve the application, that `/api/` reaches the backend, that `/actuator/` is not exposed, that the backend host port is not published, and that Redis accepts the right password and rejects a wrong one; the job shall shut the stack down afterwards.

### R8 Documentation and architecture document update

**User story:** As a developer, an installer or a reviewer, I want the architecture document and every README to describe what is actually built and how to run it, so that nothing is stale.

Acceptance criteria:

- R8.1 The root `README.md` (Spanish) shall be updated to the current state: it shall no longer say that there is no application code or that Compose starts only PostgreSQL and Redis; it shall document how to start the project in the three modes, the variables of `.env.example`, the entry points (`/admin`, `/player`), the single published web port, the repository structure (including `spec/` and `.github/`), the current technology stack, the spec-driven and gitflow workflow, and the current open decisions.
- R8.2 `apps/web/README.md` (English) shall explain that the container is started through Compose, where the reverse proxy is configured and which variables it reads.
- R8.3 `services/core-backend/README.md` shall state that the backend is internal to the Compose network, how to reach it through the web proxy, and how to check its health from inside the network.
- R8.4 `services/vision-service/README.md` shall drop the "Redis is not published" limitation and describe how a station reaches the server's Redis with the password.
- R8.5 `AGENTS.md` shall be updated in the same pull request: the compose commands, the topologies, the ports, the rule that `/api` and `/ws` are the backend routes, and the project status line.
- R8.6 The architecture document `docs/kiosko-arquitectura-mvp.md` shall be updated to a new revision that incorporates what has been developed, limited to decisions already agreed:
  - a changelog of the revision;
  - the technology stack table (current backend and web stack, nginx for the web, Redis with password on the local network);
  - the physical architecture text and both diagrams, with the `web` service, the single entry point and the player URL on the station;
  - the backend section on Clean Architecture with DDD and database independence;
  - the repository structure (`spec/`, `.github/`, `AGENTS.md`);
  - the decisions: "event channel with remote stations" moves from the open table to decided, and the TLS limitation is recorded as an accepted risk;
  - a table of implementation status by feature, and an updated "next steps" list;
  - a pointer to the campaign administration document in `docs/`.
- R8.7 Every command, port, profile, path and variable stated in these documents shall be checked against the repository in the same pull request, and anything planned but not built shall be labeled as planned.
- R8.8 The documents shall keep their language conventions: `README.md` and `docs/` in Spanish, `apps/web/README.md` and `AGENTS.md` in English, as they are today.

### R9 Constraints carried over

**User story:** As the project owner, I want the privacy, security and licensing rules to hold in the Compose setup.

Acceptance criteria:

- R9.1 Every image shall run as a non-root user.
- R9.2 No secret, `.env` file or credential shall be committed or baked into an image; `.env.example` shall contain only placeholders.
- R9.3 The setup shall not store frames, face crops or per-person data, and shall add no volume for them.
- R9.4 All images and added components shall use licenses compatible with a commercial closed product. The event bus image shall be Valkey (BSD-3-Clause), not Redis 7.4 or later.

## Out of scope

- Camera device mapping and camera sources (KIO-10, KIO-11).
- Starting Chromium in kiosk mode and auto-start of the stack on the station (KIO-45).
- A media volume and media storage (KIO-31).
- Redis client code and the audience event consumer (KIO-20, KIO-23); WebSocket orders (KIO-29).
- Backend REST endpoints under `/api` (KIO-58 onward); this feature only creates the route.
- TLS/HTTPS on the local network (see open questions).
- A copy of the web on the station for offline operation.
- Kubernetes, a cloud, or a reverse proxy other than the nginx already in the `web` image.

## Open questions (to settle in design.md)

1. How the nginx upstream host is configured without editing the file per environment (nginx image templates with environment substitution, or another way).
2. Request body limit and timeouts for uploads: concrete defaults until KIO-31 and KIO-61 decide the real limits.
3. Whether to add a CI smoke job that runs `docker compose --profile server up --wait` and checks `/admin`, `/player` and the `/api` route (recommended now that there are four services; it lengthens each pull request).
4. Whether the web port variable also applies to Compose's internal port, and how a development machine that already uses port 80 overrides it.
5. TLS: the admin login would travel in clear text on the store's LAN. Whether HTTPS is required for the MVP or accepted as a documented limitation.
6. Whether Redis needs `protected-mode` or `bind` settings beyond the password, and how the station-side verification of R4.3 is automated (for example a throwaway `redis` client container).
7. Who coordinates the change to `apps/web/nginx.conf` and `Dockerfile` with the KIO-8 author, since both belong to that feature.
8. Whether the KIO-9 Trello card should be rewritten to reflect the new scope (the original text is already satisfied by KIO-6 and KIO-7).

Accepted risk: publishing Redis opens a network service. Mitigations are R4.1 (loopback by default), R4.2 (mandatory password) and R4.6 (documentation).
