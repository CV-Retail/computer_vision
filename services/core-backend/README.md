# core-backend

Core backend of the smart kiosk: campaigns, rules engine and reports. This version is only the skeleton (KIO-7): a Spring Boot application with Spring Modulith modules, Clean Architecture layers enforced by tests, database-independent Flyway migrations and a health endpoint. It has no domain logic yet.

Design: [`spec/KIO-7-backend-skeleton/`](../../spec/KIO-7-backend-skeleton/) and [`docs/kiosko-arquitectura-mvp.md`](../../docs/kiosko-arquitectura-mvp.md).

Stack: Java 21, Spring Boot 4.1.1, Spring Modulith 2.1.1, Flyway, PostgreSQL (runtime), H2 (tests only).

## Build and test

Only Java 21 is required; the Maven Wrapper downloads Maven. From `services/core-backend`:

```bash
./mvnw -B -ntp test      # unit tests only (fast)
./mvnw -B -ntp verify    # unit tests, package, integration tests
```

Unit and integration tests are told apart by name:

| Kind | Name | Runs in | Needs |
|---|---|---|---|
| Unit | `*Test` | `./mvnw test` (Surefire) | No Spring context, database or network |
| Integration | `*IT` | `./mvnw verify` (Failsafe) | Spring context with embedded H2 |

Do not name tests `*Tests`: Surefire would run them as unit tests. Test domain and application logic with unit tests and keep integration tests few.

Run one test:

```bash
./mvnw -Dtest=ModularityTest test
./mvnw -B -ntp verify -Dit.test=HealthEndpointIT -Dtest=NoMatch -Dsurefire.failIfNoSpecifiedTests=false
```

No Docker is needed to run the tests.

## Run

The application reads all its configuration from environment variables and fails at startup, naming the variable, if one is missing:

| Variable | Meaning | Example |
|---|---|---|
| `DB_URL` | JDBC URL | `jdbc:postgresql://localhost:5432/kiosko` |
| `DB_USER` | Database user | `kiosko` |
| `DB_PASSWORD` | Database password | (from your `.env`) |
| `DB_VENDOR` | Database engine, selects `db/migration/<vendor>` | `postgresql` |

```bash
DB_URL=... DB_USER=... DB_PASSWORD=... DB_VENDOR=postgresql ./mvnw spring-boot:run
curl localhost:8080/actuator/health
```

The only exposed endpoint is `/actuator/health` (no details). If the database is unreachable it answers `DOWN` (HTTP 503) within about 5 seconds. Authentication for `/admin` arrives with KIO-32.

## Docker and Docker Compose

```bash
docker build -t kiosko-core-backend .
```

From the repository root, after `cp .env.example .env`:

- **Everything on one host:** `docker compose --profile all-in-one up --build`.
- **Server apart from the stations:** `docker compose --profile server up --build` on the server (PostgreSQL, Redis and this backend). The backend never runs under the `kiosk` profile.

Compose builds `DB_URL`, `DB_USER`, `DB_PASSWORD` and `DB_VENDOR` from the `POSTGRES_*` variables, waits for PostgreSQL to be healthy, and publishes port 8080.

## Architecture

Three Spring Modulith modules under `com.cvretail.kiosko`: `campaigns`, `rules` and `reports`. Each has the packages:

```
<module>/            published surface: the only types other modules may use
├── domain/          aggregates, value objects, domain events; Java standard library only
├── application/     use cases and repository ports (interfaces)
└── infrastructure/  adapters (database, Redis, HTTP, WebSocket); all vendor-specific code
```

Rules, enforced by tests (`ModularityTest`, `ArchitectureTest`) that fail the build and name the offending class:

- `domain` does not depend on Spring, JPA/Jakarta, Flyway, Jackson, JDBC, `application` or `infrastructure`.
- `application` does not depend on `infrastructure`.
- Nothing depends on `infrastructure` except the Spring wiring in the root package.
- Modules use each other only through their published surface (the module base package); no cycles.

`ArchitectureRulesDetectViolationsTest` runs the same rules against deliberately bad classes in `src/test/java/com/cvretail/archfixtures` to prove they fail.

## Database independence

The domain never knows the engine. Flyway loads `db/migration/common` (portable SQL) plus `db/migration/<vendor>` (engine-specific). To support a new engine, for example SQL Server:

1. Add its JDBC driver and Flyway module as dependencies.
2. Create `src/main/resources/db/migration/sqlserver/`.
3. Set `DB_VENDOR=sqlserver` and `DB_URL`.

No change is needed in `domain` or `application`.

Caveat: the tests run on H2, so passing tests do not prove compatibility with another engine. When adding an engine, run the migrations and the integration checks against that engine. Compose runs the real PostgreSQL path.

## Migrations

- Never edit an applied migration; add a new one.
- Keep migrations in `common` portable SQL; put engine-specific statements in the `<vendor>` folder.
- Version numbers are shared across both folders, so do not reuse a version that exists in `common`.
- Domain tables arrive with KIO-24 onward; the baseline `V1` creates none.

## Rules

- Never log or store frames, face crops, embeddings or per-person data.
- Do not add AGPL or non-commercial dependencies.
