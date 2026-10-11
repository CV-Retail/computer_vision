# Kiosko Retail - Web Application (`apps/web`)

The single web application for the Kiosko system built with React 18+, Vite, and TypeScript. It serves both the Store Admin Portal (`/admin`) and the Ad Player display (`/player`) for Chromium in kiosk mode.

## Architecture

The project follows **Clean Architecture & Clean Code principles** with strict separation between layers and modular page domains:

- **`src/domain/`:** Pure TypeScript business models and interfaces (zero React/DOM dependencies).
- **`src/application/`:** Application state management, use-cases, and custom hooks.
- **`src/infrastructure/`:** Network adapters, API ports, and WebSocket clients placeholder.
- **`src/presentation/`:** Modular UI presentation:
  - `shared/`: Shared visual design system, Vanilla CSS tokens, glassmorphism card primitives (`GlassCard`, `StatusBadge`).
  - `admin/`: Store Admin Portal features (`/admin`), layouts, and views.
  - `player/`: Frameless Ad Player features (`/player`), full-screen kiosk layouts, and views.

## Prerequisites

- Node.js 20+
- npm 10+

## Local Development

Install dependencies:

```bash
npm ci
```

Start the Vite development server:

```bash
npm run dev
```

The application will be available at `http://localhost:3000` (`/admin` and `/player`).

## Production Build, Typecheck, and Linting

- **Typecheck:** `npm run typecheck`
- **Lint:** `npm run lint`
- **Build static bundle:** `npm run build` (outputs to `dist/`)

## Testing

Run unit & component tests in single-run CI mode:

```bash
npm run test
```

Run a single test file:

```bash
npm run test -- src/test/App.test.tsx
```

## Docker and Docker Compose

Build the multi-stage image (Node build, then unprivileged nginx):

```bash
docker build -t kiosko-web .
```

The normal way to run it is through Compose, from the repository root (`cp .env.example .env` first):

```bash
docker compose --profile server up --build      # or --profile all-in-one
```

The `web` service is the only application port published on the host: `http://localhost/admin` and `http://localhost/player` (the host port is `WEB_PORT`, default 80). It runs only on the server side; a station opens `http://<server>/player` in Chromium.

### Reverse proxy

nginx is configured in `nginx/default.conf.template`, rendered at container start from environment variables:

| Route | Goes to |
|---|---|
| `/api/` | the backend (`BACKEND_HOST:BACKEND_PORT`), path unchanged, errors passed through |
| `/ws` | the backend, with the WebSocket upgrade headers |
| `/actuator/` | `404` (the backend health endpoint is never exposed) |
| anything else | the single-page application (`index.html` fallback) |

| Variable | Default | Meaning |
|---|---|---|
| `BACKEND_HOST` | `core-backend` | Backend host name |
| `BACKEND_PORT` | `8080` | Backend port |
| `DNS_RESOLVER` | `127.0.0.11` | DNS server used to resolve the backend at request time |
| `CLIENT_MAX_BODY_SIZE` | `100m` | Upload limit (provisional until media uploads are built) |

The backend is resolved at request time, so it can be recreated without restarting this container.

Check the nginx configuration without a backend:

```bash
docker run --rm \
  -v "$PWD/nginx/default.conf.template:/etc/nginx/templates/default.conf.template:ro" \
  -e BACKEND_HOST=core-backend -e BACKEND_PORT=8080 -e DNS_RESOLVER=127.0.0.11 -e CLIENT_MAX_BODY_SIZE=100m \
  nginxinc/nginx-unprivileged:alpine nginx -t
```

Running the image alone (`docker run -p 8080:8080 kiosko-web`) serves the application at `http://localhost:8080/admin` and `/player`, but `/api` answers `502` because there is no backend.
