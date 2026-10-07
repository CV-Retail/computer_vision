# KIO-8 Web application skeleton: design

Implements `requirements.md` (R1–R8). Scope is the frontend application skeleton for `apps/web`: React + Vite + TypeScript project structure adhering to **Clean Architecture & Clean Code principles**, modular independent page domains (`admin` and `player`), SPA routing (`/admin` and `/player`), target-decoupled build capability, Vanilla CSS design system with dark glassmorphism aesthetics, Vitest + React Testing Library test setup, static build setup, multi-stage Dockerfile, CI job activation, and documentation updates. No backend integration or campaign logic.

## 1. Architecture & Clean Code Structure

The application is structured following Clean Architecture principles adapted for modern React applications, with strict separation between page feature domains (`admin` and `player`):

1. **Domain Layer (`src/domain/`):** Core business models, interfaces, and domain types. Completely framework-agnostic (pure TypeScript, zero React or HTTP dependencies).
2. **Application Layer (`src/application/`):** Application use-cases, business workflows, state abstractions, and custom hooks. Manages application state without direct UI rendering.
3. **Infrastructure Layer (`src/infrastructure/`):** External adapters, API clients, WebSocket ports, local storage handlers, and device interaction logic.
4. **Presentation Layer (`src/presentation/`):** Modular presentational architecture:
   - **`shared/`:** Shared visual design tokens, CSS custom properties, and UI primitives (`GlassCard`, `StatusBadge`).
   - **`admin/`:** Completely decoupled Admin Portal features (`views/`, `components/`, `layouts/`).
   - **`player/`:** Completely decoupled Ad Player features (`views/`, `components/`, `layouts/`). Zero dependencies on `admin/`.

```
apps/web/
├── package.json
├── index.html
├── vite.config.ts
├── tsconfig.json
├── tsconfig.app.json
├── tsconfig.node.json
├── eslint.config.js
├── README.md
├── Dockerfile
├── nginx.conf
├── .dockerignore
├── .gitignore
├── public/
│   └── favicon.ico
├── src/
│   ├── domain/                      (Layer 1: Pure domain models & TypeScript types)
│   │   ├── kiosk.types.ts
│   │   └── player.types.ts
│   ├── application/                 (Layer 2: Application state abstractions & custom hooks)
│   │   └── useKioskStatus.ts
│   ├── infrastructure/              (Layer 3: External adapters, network/storage placeholders)
│   │   └── api/
│   ├── presentation/                (Layer 4: Modular UI presentation)
│   │   ├── shared/                  (Shared UI primitives & tokens)
│   │   │   ├── components/
│   │   │   │   ├── GlassCard.tsx
│   │   │   │   ├── GlassCard.css
│   │   │   │   ├── StatusBadge.tsx
│   │   │   │   └── StatusBadge.css
│   │   │   └── index.css            (Global design system & tokens)
│   │   ├── admin/                   (Independent Admin domain)
│   │   │   ├── layouts/
│   │   │   │   ├── AdminLayout.tsx
│   │   │   │   └── AdminLayout.css
│   │   │   └── views/
│   │   │       └── AdminDashboardView.tsx
│   │   └── player/                  (Independent Player domain - frameless kiosk)
│   │       ├── layouts/
│   │       │   ├── PlayerLayout.tsx
│   │       │   └── PlayerLayout.css
│   │       └── views/
│   │           └── PlayerDisplayView.tsx
│   ├── router.tsx                   (SPA Route configuration mapping /admin, /player, fallback)
│   ├── main.tsx
│   ├── App.tsx
│   └── test/                        (Automated test suite)
│       ├── setup.ts                 (Testing library extensions & Vitest environment setup)
│       └── App.test.tsx             (Clean architecture component & route tests)
```

Files changed outside `apps/web/`: `.github/workflows/ci.yml` (verified job activation), `AGENTS.md`.

## 2. Decisions on the open questions

| Question | Decision | Why |
|---|---|---|
| Clean Architecture in Frontend | Layered separation (`domain`, `application`, `infrastructure`, `presentation`) | Promotes high cohesion, low coupling, SRP (Single Responsibility Principle), and clean code structure. |
| Page Independence | Modular folders (`presentation/admin` vs `presentation/player`) with zero cross-page imports | Enables running admin and player independently or packaging them in separate Dockerfiles. |
| Docker Multi-Target Capability | Single Dockerfile supporting build arg `BUILD_TARGET=all|admin|player` | Allows building a single SPA for `all-in-one` profile, or standalone decoupled images for kiosk stations. |
| Routing library | `react-router-dom` (v6/v7) | Standard React SPA router; lightweight, component-driven, compatible with Vitest + JSDOM testing. |
| Bundler & Plugins | Vite + `@vitejs/plugin-react` | Fast dev server, instant HMR, clean static output in `dist/`. |
| Test Runner | Vitest + React Testing Library + `@testing-library/jest-dom` + `jsdom` | Vitest seamlessly shares Vite configuration, executes rapidly, and provides full DOM assertion support. |
| Single test syntax | `npm run test -- src/test/App.test.tsx` | Vitest natively supports passing file patterns in non-watch execution mode (`vitest run`). |
| CSS Architecture | Vanilla CSS with CSS custom properties in `presentation/shared/index.css` and module-scoped CSS | Adheres to project design guidelines (Vanilla CSS, vibrant dark palette, glassmorphism, custom typography, no heavy CSS utility dependencies). |
| Docker Base Image | `nginxinc/nginx-unprivileged:alpine` | Non-root by default, secure, lightweight (~15MB), multi-arch (amd64 and arm64 support). |
| Docker Routing | `nginx.conf` with `try_files $uri $uri/ /index.html` | Solves SPA client-side routing fallback so `/admin` and `/player` reload cleanly in production containers. |

## 3. Detailed Component Specification & Layer Rules

### 3.1 Domain & Application Layers Clean Code Rules (R1, R4)

- **Domain (`src/domain/`):** Zero dependencies on React, DOM APIs, or HTTP libraries. Contains clean TypeScript interfaces (e.g. `KioskInfo`, `PlayerPlaybackState`).
- **Application (`src/application/`):** Encapsulates state management and business rules into custom hooks and services. Presentational components consume hooks without managing raw business rules directly.

`package.json` dependencies and configuration:
- Core dependencies: `react` (^18.3.1), `react-dom` (^18.3.1), `react-router-dom` (^6.28.0).
- Dev dependencies: `typescript` (^5.6.3), `vite` (^5.4.10), `@vitejs/plugin-react` (^4.3.3), `@types/react` (^18.3.12), `@types/react-dom` (^18.3.1), `@types/node` (^22.9.0), `eslint` (^9.14.0), `vitest` (^2.1.4), `jsdom` (^25.0.1), `@testing-library/react` (^16.0.1), `@testing-library/jest-dom` (^6.6.3).
- License verification: All packages use MIT, Apache-2.0, or BSD licenses. No AGPL or non-commercial packages are introduced.
- Scripts:
  - `npm run dev`: starts Vite dev server.
  - `npm run build`: runs `tsc -b && vite build` to generate `apps/web/dist`.
  - `npm run lint`: runs ESLint over `src/`.
  - `npm run typecheck`: runs `tsc --noEmit`.
  - `npm run test`: runs `vitest run` in single-run CI mode.

### 3.2 Presentation Layer & Routing (R2)

Defined in `src/router.tsx` and `src/presentation/`:
- Base routes:
  - `/admin`: Renders `<AdminLayout />` containing top navigation header (Logo, Store Title, Status indicator) and sidebar navigation (Dashboard, Campaigns, Rules, Reports), with content outlet rendering `<AdminDashboardView />`.
  - `/player`: Renders `<PlayerLayout />` taking up `100vw` and `100vh` with `overflow: hidden`, dark background, zero margins, and content outlet rendering `<PlayerDisplayView />` (displaying playback status skeleton and active campaign preview placeholder).
  - `/` and unknown routes: Redirection to `/admin` or fallback to `<NotFoundView />`.

Clean Code UI Guidelines:
- Modular feature folders (`presentation/admin` and `presentation/player`).
- Presentational components are small, focused, and reusable (`GlassCard`, `StatusBadge`).
- Components receive explicitly typed props from `src/domain/`.

### 3.3 Design System & CSS Foundation (R3)

Defined in `src/presentation/shared/index.css`:
- Custom properties for design tokens:
  ```css
  :root {
    --bg-dark: #0a0d12;
    --bg-card: rgba(22, 27, 34, 0.75);
    --bg-card-hover: rgba(30, 38, 48, 0.85);
    --border-glass: rgba(255, 255, 255, 0.08);
    --border-accent: rgba(88, 166, 255, 0.3);
    --text-primary: #f0f6fc;
    --text-secondary: #8b949e;
    --accent-blue: #58a6ff;
    --accent-green: #3fb950;
    --accent-purple: #bc8cff;
    --shadow-glass: 0 8px 32px 0 rgba(0, 0, 0, 0.36);
    --radius-sm: 6px;
    --radius-md: 12px;
    --radius-lg: 18px;
    --font-family: 'Inter', system-ui, -apple-system, BlinkMacSystemFont, sans-serif;
  }
  ```
- Surface & Glassmorphism styles:
  - `.glass-panel`: Background blur (`backdrop-filter: blur(12px)`), translucent background, elevated border, subtle shadow.
  - Hover effects & transition micro-animations.

### 3.4 Code Quality & Static Checks (R4)

- `tsconfig.json`: Extends `tsconfig.app.json` with `"strict": true`, `"jsx": "react-jsx"`, `"moduleResolution": "Bundler"`, `"noUnusedLocals": true`, `"noUnusedParameters": true`.
- ESLint configuration: Strict React hooks rules, type-aware lint checks, unused variable detection.

### 3.5 Automated Testing Setup (R5)

- `vite.config.ts` includes `test` configuration:
  ```typescript
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: './src/test/setup.ts',
  }
  ```
- `src/test/setup.ts`: Imports `@testing-library/jest-dom/vitest`.
- `src/test/App.test.tsx`:
  - Test 1: Verifies `/admin` route renders Admin header and sidebar navigation elements.
  - Test 2: Verifies `/player` route renders full-screen player layout with player status skeleton.
  - Test 3: Verifies unknown routes redirect cleanly to fallback view.

### 3.6 CI Integration (R6)

- The repository CI workflow `.github/workflows/ci.yml` already contains the `Web` job which triggers when `apps/web/package.json` exists.
- The workflow executes:
  1. `npm ci`
  2. `npm run lint`
  3. `npm run typecheck`
  4. `npm run test`
  5. `npm run build`
- All commands will execute and pass cleanly.

### 3.7 Documentation (R7)

- `apps/web/README.md`: Explains installation (`npm install`), development server (`npm run dev`), static build (`npm run build`), linting, typechecking, running tests (`npm run test`), single-test execution (`npm run test -- src/test/App.test.tsx`), Clean Architecture layer principles, and Docker container commands.
- `AGENTS.md`: Update status line to reflect `apps/web` skeleton completion and detail npm commands.

### 3.8 Container Image & Docker Support (R8)

`apps/web/Dockerfile`:
```dockerfile
# Stage 1: Build
FROM node:20-alpine AS build
ARG BUILD_TARGET=all
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build -- --mode ${BUILD_TARGET}

# Stage 2: Runtime
FROM nginxinc/nginx-unprivileged:alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=3s --start-period=5s --retries=3 \
  CMD wget -qO- http://localhost:8080/ || exit 1
```

`apps/web/nginx.conf`:
```nginx
server {
    listen 8080;
    server_name localhost;
    root /usr/share/nginx/html;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }
}
```

- Target architectures: `amd64`, `arm64`.
- Supports `--build-arg BUILD_TARGET=all|admin|player`.
- Runs under unprivileged `101:101` user.

## 4. Traceability Matrix

| Requirement ID | Design Section | Description / Implementation Mapping |
|---|---|---|
| **R1.1 – R1.4** | 3.1 Project Setup & Scripts | `package.json`, Vite, React 18+, TypeScript 5+, commercial licenses. |
| **R2.1 – R2.4** | 3.2 Presentation Layer & Routing | `src/router.tsx`, `AdminLayout.tsx`, `PlayerLayout.tsx`, `/admin` and `/player` routes. |
| **R3.1 – R3.3** | 3.3 Design System & CSS | `src/presentation/shared/index.css`, CSS variables, dark glassmorphism aesthetic. |
| **R4.1 – R4.3** | 3.4 Code Quality | `tsconfig.json` (`strict: true`), ESLint configuration. |
| **R5.1 – R5.4** | 3.5 Automated Testing Setup | Vitest, React Testing Library, `App.test.tsx`, single test execution. |
| **R6.1 – R6.2** | 3.6 CI Integration | `.github/workflows/ci.yml` `Web` job activation. |
| **R7.1 – R7.2** | 3.7 Documentation | `apps/web/README.md` and `AGENTS.md` updates. |
| **R8.1 – R8.4** | 3.8 Container Image & Docker | Multi-stage `Dockerfile` with `BUILD_TARGET` arg, `nginx.conf` SPA fallback, non-root user, healthcheck. |
