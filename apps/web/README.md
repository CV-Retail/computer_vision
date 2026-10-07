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

## Docker Containerization

Build the multi-stage Docker image using unprivileged Nginx:

```bash
docker build -t kiosko-web .
```

Run the container locally:

```bash
docker run -p 8080:8080 kiosko-web
```

Open `http://localhost:8080/admin` or `http://localhost:8080/player`.
