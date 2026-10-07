# KIO-8 Web application skeleton: requirements

## Introduction

`apps/web` is the single web application built with React, Vite, and TypeScript that serves the two user-facing interfaces of the kiosk system: the Store Admin Portal (`/admin`) and the Ad Player display (`/player`) for Chromium in kiosk mode (see `docs/kiosko-arquitectura-mvp.md`).

This feature establishes the project skeleton for `apps/web`: a clean, type-safe, testable, containerized, and buildable React + Vite + TypeScript SPA structure. It provides the foundation for future frontend capabilities (campaign management, player orders, local login, analytics reports) and activates the `Web` job in the project CI workflow (`.github/workflows/ci.yml`).

Goals:
- Initialize a React 18+, Vite, and TypeScript 5+ SPA project in `apps/web`.
- Implement SPA routing distinguishing `/admin` (Store Admin Portal) and `/player` (Ad Player kiosk screen).
- Define required npm scripts: `dev`, `build`, `lint`, `typecheck`, `test`.
- Activate and ensure passing execution of the `Web` CI job in `.github/workflows/ci.yml`.
- Build a Vanilla CSS design system with CSS custom properties, responsive layout primitives, modern dark aesthetics, and glassmorphism/elevated card foundations.
- Set up Vitest and React Testing Library for fast unit and component testing.
- Provide a multi-stage `Dockerfile` to containerize and serve `apps/web` using Docker.
- Document development workflow in `apps/web/README.md` and update `AGENTS.md`.

Source: Trello card KIO-8, `docs/kiosko-arquitectura-mvp.md`, and project workflow rules in `AGENTS.md`.

## Requirements

### R1 Project initialization and build configuration

**User story:** As a developer, I want a standard React + Vite + TypeScript project structure in `apps/web` so that the application builds reproducibly and fast.

Acceptance criteria:
- R1.1 The service shall be defined by `apps/web/package.json` with TypeScript 5+, Vite, React 18+, and React Router.
- R1.2 The project shall provide standard npm scripts: `dev` (dev server), `build` (production bundle), `lint` (linter check), `typecheck` (TypeScript compiler check), and `test` (test suite run).
- R1.3 When `npm run build` is executed in `apps/web`, the system shall compile TypeScript and bundle static production assets into `apps/web/dist` without errors.
- R1.4 All dependencies shall use licenses compatible with a commercial closed product (MIT, Apache-2.0, BSD). No AGPL or non-commercial dependencies shall be included.

### R2 Routing and core layout structure

**User story:** As a user or kiosk operator, I want the web app to host both the admin portal and the ad player using distinct routes.

Acceptance criteria:
- R2.1 The application shall implement SPA routing supporting `/admin` (Store Admin Portal) and `/player` (Ad Player), with default redirection or fallback for `/` or unknown routes.
- R2.2 While navigating to `/admin`, the system shall render an Admin Layout skeleton featuring header navigation, sidebar navigation, and a placeholder content area.
- R2.3 While navigating to `/player`, the system shall render a full-screen, frameless Player Layout skeleton with hidden scrollbars, optimized for Chromium kiosk display.
- R2.4 The `/player` view shall include a placeholder status display showing idle / playback skeleton state.

### R3 Design system and CSS foundation

**User story:** As a user, I want a modern, polished visual design system so that both the admin portal and ad player look high-end and premium.

Acceptance criteria:
- R3.1 The application shall use Vanilla CSS with CSS custom properties (variables) for design tokens (colors, typography, spacing, shadows, border-radius).
- R3.2 The design system shall implement dark mode styles with curated color palettes, glassmorphism cards, fluid layout containers, and smooth micro-interactions.
- R3.3 CSS files shall be organized cleanly (design tokens, reset/base rules, layout styles, component styles).

### R4 Code quality, linting, and type checking

**User story:** As a developer, I want strict static checks so that bugs and formatting inconsistencies are caught early.

Acceptance criteria:
- R4.1 TypeScript configuration (`tsconfig.json`) shall enforce strict mode (`"strict": true`).
- R4.2 ESLint configuration shall enforce React rules, hooks guidelines, and formatting standard checks.
- R4.3 When `npm run lint` or `npm run typecheck` is executed in `apps/web`, the system shall complete with exit code 0 if clean, or fail with a non-zero exit code if errors exist.

### R5 Testing framework setup

**User story:** As a developer, I want an automated test setup so component and route regressions can be prevented.

Acceptance criteria:
- R5.1 The application shall configure Vitest and React Testing Library for testing React components.
- R5.2 The test suite shall include unit tests verifying route navigation (`/admin` and `/player`) and core layout rendering.
- R5.3 When `npm run test` is executed, the system shall run all unit tests in CI mode (non-watch) and return code 0 on success.
- R5.4 The test suite shall support running a single test file (e.g. `npm run test -- src/App.test.tsx`).

### R6 CI Integration

**User story:** As a team member, I want CI to build and test `apps/web` on every PR.

Acceptance criteria:
- R6.1 When `apps/web/package.json` exists, the `Web` job in `.github/workflows/ci.yml` shall run `npm ci`, `npm run lint`, `npm run typecheck`, `npm run test`, and `npm run build` without modifications to CI logic.
- R6.2 If any of the web scripts fail, the `CI success` check shall fail.

### R7 Documentation and project guidelines update

**User story:** As a developer, I want updated documentation on how to build, test, run, and containerize the web app.

Acceptance criteria:
- R7.1 `apps/web/README.md` shall document prerequisites, dev server startup, production build, linting, typechecking, test suite execution, single-test execution, Docker build/run, and directory structure.
- R7.2 `AGENTS.md` shall be updated with verified web commands, project status updates, and web app conventions.

### R8 Container image and Docker support

**User story:** As an operator or developer, I want a Dockerfile and container support so that `apps/web` can be built and served inside Docker environments.

Acceptance criteria:
- R8.1 The application shall include a multi-stage `Dockerfile` in `apps/web` that compiles static assets with Node.js and serves them using a lightweight web server (e.g., Nginx unprivileged), published for amd64 and arm64 architectures.
- R8.2 When `docker build` is executed in `apps/web`, the system shall produce a container image that serves the SPA and correctly handles SPA client-side fallback routing (`/admin` and `/player`).
- R8.3 The container shall run as a non-root user and expose an HTTP port (e.g., 8080).
- R8.4 The container shall define a health check to verify the HTTP server status.

## Out of Scope

- Backend static file serving integration in `services/core-backend` (KIO-35).
- Real-time WebSocket connection to the backend for player orders (KIO-29 / KIO-35).
- Campaign management CRUD operations, media uploads, or backend authentication (KIO-30, KIO-31, KIO-32).
- Analytics reports UI and charts (KIO-33).

## Open Questions (to settle in `design.md`)

1. Routing library choice (`react-router-dom` v6/v7 vs lightweight router).
2. Vite configuration and plugins (`@vitejs/plugin-react`).
3. Vitest setup and single test command syntax.
4. CSS structure and token distribution across `/admin` and `/player`.
5. Web server image selection for Docker runtime (e.g., `nginxinc/nginx-unprivileged` vs static node server).
