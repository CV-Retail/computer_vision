# KIO-8 Web application skeleton: tasks

Implements `design.md` for `requirements.md`. Do the tasks in order and tick each one when done. Commands run from `apps/web` unless a task says "repo root". Do not commit, push or open a PR until the user asks.

## Tasks

- [x] **T1 Project initialization & dependencies** (R1.1–R1.4, R4.1)
  Initialize `apps/web/package.json` with React 18+, React Router 6+, TypeScript 5+, Vite 5/6, ESLint, Vitest, JSDOM, and React Testing Library. Create `tsconfig.json`, `tsconfig.app.json`, `tsconfig.node.json`, `vite.config.ts`, `eslint.config.js`, and `.gitignore`. Mark all dependency licenses as commercial-compatible (MIT, Apache-2.0, BSD).
  *Verify:* `npm install` and `npm ci` in `apps/web` install dependencies cleanly.

- [x] **T2 Design System & Vanilla CSS Tokens** (R3.1–R3.3)
  Create `src/presentation/shared/index.css` defining root CSS custom properties (`--bg-dark`, `--bg-card`, `--text-primary`, `--accent-blue`, `--shadow-glass`, `--font-family`), glassmorphism utility classes (`.glass-panel`), hover micro-interactions, responsive flex/grid layout primitives, and base resets.
  *Verify:* CSS parses cleanly without syntax errors during Vite build.

- [x] **T3 Domain Types & Reusable UI Primitives** (R1.1, R3.1–R3.3, R4.1)
  Create `src/domain/kiosk.types.ts` and `src/domain/player.types.ts` defining domain interfaces (`KioskStatus`, `PlaybackItem`). Create reusable presentation components in `src/presentation/shared/components/`: `GlassCard.tsx` / `GlassCard.css` and `StatusBadge.tsx` / `StatusBadge.css`.
  *Verify:* TypeScript check (`npm run typecheck`) passes with zero errors.

- [x] **T4 Page Layouts & Modular Views** (R2.1–R2.4)
  Create decoupled layouts and page views following clean feature separation:
  - `src/presentation/admin/layouts/AdminLayout.tsx` and `AdminLayout.css` (Top navbar with logo & status, sidebar with Dashboard/Campaigns/Rules/Reports, main content outlet).
  - `src/presentation/player/layouts/PlayerLayout.tsx` and `PlayerLayout.css` (Full-screen `100vw`/`100vh` frameless kiosk layout, hidden scrollbars).
  - `src/presentation/admin/views/AdminDashboardView.tsx` (Placeholder dashboard with elevated glassmorphism cards & status metrics).
  - `src/presentation/player/views/PlayerDisplayView.tsx` (Placeholder player screen showing active playback state skeleton).
  - `src/presentation/shared/views/NotFoundView.tsx`.
  *Verify:* TypeScript typecheck passes for all layout components.

- [x] **T5 Routing & Application Entrypoint** (R2.1, R2.4)
  Create `src/router.tsx` configuring SPA routes for `/admin` (rendering `AdminLayout` & `AdminDashboardView`), `/player` (rendering `PlayerLayout` & `PlayerDisplayView`), and `*` fallback. Create `src/App.tsx`, `src/main.tsx`, `public/favicon.ico`, and `index.html`.
  *Verify:* SPA route configuration and entrypoints render properly.

- [x] **T6 Automated Testing Setup & Suite** (R5.1–R5.4)
  Configure Vitest in `vite.config.ts`, create `src/test/setup.ts` (extending `@testing-library/jest-dom`), and write `src/test/App.test.tsx` verifying:
  1. Navigating to `/admin` renders AdminLayout header and navigation elements.
  2. Navigating to `/player` renders PlayerLayout full-screen kiosk display.
  3. Unknown routes render 404 / fallback view.
  4. Test single-test execution syntax (`npm run test -- src/test/App.test.tsx`).
  *Verify:* `npm run test` executes in CI mode and passes 3/3 tests with exit code 0.

- [x] **T7 Static Build & Static Analysis Checks** (R1.2–R1.4, R4.1–R4.3)
  Run `npm run lint`, `npm run typecheck`, and `npm run build` in `apps/web/`. Confirm static bundle output in `apps/web/dist`.
  *Verify:* `apps/web/dist/index.html` and static JS/CSS bundles exist, exit code 0 for all scripts.

- [x] **T8 Container Image, Nginx & Docker Support** (R8.1–R8.4)
  Create `apps/web/Dockerfile`, `apps/web/nginx.conf`, and `apps/web/.dockerignore`. Implement multi-stage build (Node 20 build -> Nginx unprivileged runtime), supporting `BUILD_TARGET` arg, non-root user execution (`101:101`), SPA routing fallback `try_files $uri $uri/ /index.html`, and container `HEALTHCHECK`. Build container (`docker build -t kiosko-web .`) and test container execution.
  *Verify:* Container builds, non-root uid 101 confirmed.

- [x] **T9 CI Workflow Integration Verification** (R6.1–R6.2; repo root)
  Verify that the `Web` job in `.github/workflows/ci.yml` correctly targets `apps/web/` and runs `npm ci`, `npm run lint`, `npm run typecheck`, `npm run test`, `npm run build`.
  *Verify:* GitHub Actions workflow YAML parses cleanly and detects `package.json`.

- [x] **T10 Documentation & AGENTS.md Update** (R7.1–R7.2)
  Write `apps/web/README.md` documenting installation, dev server startup, static build, linting, typechecking, test suite execution, single-test execution syntax, Clean Architecture layer principles, and Docker build commands. Update `AGENTS.md` project status line and web commands section.
  *Verify:* All commands documented in README are verified and executable.

- [x] **T11 Dependency License & Privacy Audit** (R1.4)
  Audit `package.json` dependencies for non-commercial or AGPL licenses. Confirm zero person identification, frame logging, or non-commercial assets in `apps/web`.
  *Verify:* License list confirmed (all MIT, Apache-2.0, or BSD).

- [x] **T12 Final Pass & Verification Report**
  Run full verification suite in `apps/web`: `npm run lint`, `npm run typecheck`, `npm run test`, `npm run build`, and `docker build`. Confirm `git status` shows only expected files. Tick all tasks in `tasks.md`.

## Requirement Coverage

| Requirement | Tasks |
|---|---|
| R1 Project initialization & build | T1, T3, T7, T11 |
| R2 Routing & layouts | T4, T5 |
| R3 Design system & CSS | T2, T3 |
| R4 Code quality & static checks | T1, T3, T7 |
| R5 Automated testing setup | T6 |
| R6 CI Integration | T9 |
| R7 Documentation & AGENTS.md | T10 |
| R8 Container image & Docker | T8 |

## Verification Results (2026-10-07)

- **Lint:** `npm run lint` passed with 0 errors / 0 warnings.
- **Typecheck:** `npm run typecheck` passed with exit code 0.
- **Tests:** `npm run test` (Vitest + JSDOM) passed 3/3 tests (duration 1.47s).
- **Single Test Execution:** `npm run test -- src/test/App.test.tsx` passed with exit code 0.
- **Static Build:** `npm run build` generated `dist/` (HTML: 0.59 kB, CSS: 7.78 kB, JS: 171.29 kB) with exit code 0.
- **Docker Image:** `docker build -t kiosko-web .` succeeded; `docker run --rm --entrypoint id kiosko-web -u` confirmed non-root execution (UID 101).
- **CI Integration:** `.github/workflows/ci.yml` `Web` job validated.
