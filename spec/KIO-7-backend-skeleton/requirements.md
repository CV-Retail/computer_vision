# KIO-7 Core backend skeleton: requirements

## Introduction

`services/core-backend` is the Java service that will hold the business domain of the kiosk: campaigns, the rules engine and the reports (see `docs/kiosko-arquitectura-mvp.md`). This feature creates only its skeleton: a buildable, testable, containerized Spring Boot project with Spring Modulith module boundaries, Clean Architecture layers enforced by tests, database-independent migrations and an Actuator health endpoint, wired into Docker Compose for the server side. It contains no domain logic.

Goals:

- Give the next backend features (KIO-23 event consumer, KIO-24 campaign model, KIO-25 to KIO-29 rules and player orders, KIO-30 to KIO-34) a stable place to land, with module boundaries and architecture rules already enforced.
- Build the backend under **Clean Architecture with DDD**: a domain free of frameworks, use cases in an application layer, and technology in an infrastructure layer.
- Keep the database **swappable**: PostgreSQL today, SQL Server or another engine tomorrow, without touching domain or application code.
- Turn on the `Core backend` job in CI (`.github/workflows/ci.yml`), which today skips because `pom.xml` does not exist.
- Keep the two supported topologies working: the backend runs with the `all-in-one` and `server` profiles and never with `kiosk`.

Source: Trello card KIO-7, `docs/kiosko-arquitectura-mvp.md` and the team's decisions in this spec. Lessons from KIO-6 are applied: Dockerfile, compose service, README and `AGENTS.md` commands are part of the feature.

## Requirements

### R1 Maven project definition

**User story:** As a developer, I want a standard Maven project so that the backend builds reproducibly on any machine and in CI.

Acceptance criteria:

- R1.1 The service shall be defined by `services/core-backend/pom.xml`, shall target Java 21, and shall use the current stable Spring Boot 4 line with the Spring Modulith 2 line (exact versions are confirmed against Spring's published releases in `design.md`, not assumed).
- R1.2 The service shall include the Maven Wrapper. When a developer runs `./mvnw -B -ntp verify` in `services/core-backend`, the system shall compile the code, run the tests and package the application with no prerequisite other than Java 21.
- R1.3 Dependency versions shall come from the Spring Boot and Spring Modulith dependency management; the build shall not pin versions that were not verified.
- R1.4 All dependencies shall use licenses compatible with a commercial closed product (no AGPL, no non-commercial license).
- R1.5 The Maven coordinates shall be `com.cvretail:core-backend` and the base package shall be `com.cvretail.kiosko`.

### R2 Spring Modulith module structure

**User story:** As a developer, I want the module boundaries defined and enforced from the start so that Campaigns, Rules and Reports do not become entangled.

Acceptance criteria:

- R2.1 The application shall have exactly three application modules, `campaigns`, `rules` and `reports`, each as a top-level package under the base package.
- R2.2 Each module shall contain only documented placeholders and no domain logic.
- R2.3 When the test suite runs, the system shall verify the module structure with Spring Modulith (`ApplicationModules.verify()`), and the build shall fail if a module accesses another module's internals or if a cyclic dependency exists.
- R2.4 The module structure shall be described in the service README.
- R2.5 Each module shall contain the packages `domain`, `application` and `infrastructure`, empty and documented with a package description, and shall separate the published surface of the module (what other modules may use) from its internals.

### R3 Database migrations

**User story:** As a developer, I want schema changes managed by migrations so that every environment has the same database.

Acceptance criteria:

- R3.1 The service shall use Flyway with a first baseline migration `V1__...` in the locations defined by R13.
- R3.2 When the application starts against an empty database of a supported engine, the system shall apply all pending migrations before serving requests.
- R3.3 If a migration fails, then the application shall fail to start.
- R3.4 The baseline migration shall not create domain tables (those belong to later features); it shall only establish the baseline.
- R3.5 The README and `AGENTS.md` shall state that applied migrations are never edited.
- R3.6 Migrations in `common` shall be portable SQL; engine-specific statements shall live only in the `<vendor>` folders.

### R4 Configuration

**User story:** As an operator, I want all environment-specific values to come from environment variables so that the same image runs everywhere.

Acceptance criteria:

- R4.1 The database connection and the database vendor (R13.2) shall be configured from the environment variables `DB_URL`, `DB_USER`, `DB_PASSWORD` and `DB_VENDOR`, so that no engine-specific value is hard-coded; Docker Compose shall derive them from the existing `POSTGRES_USER`, `POSTGRES_PASSWORD` and `POSTGRES_DB` variables of `.env.example`.
- R4.2 The repository shall not contain passwords, tokens or default credentials usable outside local development.
- R4.3 If a required variable is missing, then the application shall fail to start with an error that names the variable.
- R4.4 Tests shall use the embedded H2 database and never a real one.

### R5 Health endpoint

**User story:** As an operator, I want a health endpoint so that Docker and the status page can tell whether the backend and its database are alive.

Acceptance criteria:

- R5.1 While the application runs, the system shall expose Spring Boot Actuator's health endpoint, including the database status.
- R5.2 The system shall expose no other Actuator endpoint, and no other HTTP endpoint, in this feature.
- R5.3 If the database is unreachable, then the health endpoint shall report the service as down.
- R5.4 The health endpoint shall not require credentials in this feature; authentication for `/admin` is KIO-32. The health response shall not disclose configuration details or secrets.

### R6 Tests

**User story:** As a developer, I want working automated tests so that every later feature adds tests from the first day.

Acceptance criteria:

- R6.1 The suite shall include a test that the application context starts and applies the migrations (`common` plus the vendor folder used by the tests) on the embedded H2 database.
- R6.2 The suite shall include the Spring Modulith structure verification from R2.3.
- R6.3 The suite shall include a test of the health endpoint.
- R6.4 All tests shall run in CI and on any developer machine with Java 21, without Docker and without services started by hand.
- R6.5 If a test fails, then `./mvnw verify` shall exit with a non-zero status so that CI fails.
- R6.6 The architecture rules of R12 shall run as automated tests in the normal `verify`.
- R6.7 Because the tests run on H2, the compose start in R9.2 shall be the check that the migrations run on real PostgreSQL, and its result shall be recorded in the task verification.
- R6.8 Unit tests shall be named `*Test`, run in the `test` phase (`./mvnw test`) through Surefire, and shall not start a Spring context, a database or a network connection.
- R6.9 Integration tests shall be named `*IT`, run in the `integration-test` phase through Failsafe, and may start the Spring context with the embedded H2 database.
- R6.10 `./mvnw verify` shall run both kinds and fail if either fails; `./mvnw test` shall run only the unit tests.

### R7 CI activation

**User story:** As a team member, I want CI to build and test the backend on every pull request so that regressions are caught before merging into `develop`.

Acceptance criteria:

- R7.1 When `services/core-backend/pom.xml` exists, the `Core backend` job shall build and test the backend with the wrapper (`./mvnw -B -ntp verify`) and pass; the workflow may change from `mvn` to `./mvnw` if `design.md` decides so.
- R7.2 If the build or a test fails, then the `CI success` check shall fail.
- R7.3 The Docker Compose job shall continue to validate all three profiles with the backend service declared.

### R8 Container image

**User story:** As an operator, I want a container image so that the backend runs identically on the server in either topology.

Acceptance criteria:

- R8.1 The service shall have a `Dockerfile` that builds the application and produces a runtime image based on a Java 21 JRE, published for both amd64 and arm64.
- R8.2 When `docker build` runs on `services/core-backend`, the system shall produce an image that starts the application.
- R8.3 The container shall run as a non-root user.
- R8.4 The image shall not contain secrets, `.env` files or local build caches.
- R8.5 The container shall define a health check that uses the health endpoint from R5.

### R9 Compose service on the server side

**User story:** As an operator, I want the backend to start with Docker Compose in the server-side profiles so that I can run everything on one host or the server apart from the stations.

Acceptance criteria:

- R9.1 The compose file shall declare `core-backend` under the profiles `all-in-one` and `server`, and shall not declare it under `kiosk`.
- R9.2 When `docker compose --profile all-in-one up` or `--profile server up` runs, the system shall build and start the backend after PostgreSQL is ready, and the backend shall apply its migrations on PostgreSQL and report healthy.
- R9.3 The backend shall publish its HTTP port on the host so the admin portal can later be reached from a laptop or tablet on the local network.
- R9.4 The database variables shall come from `.env`, with `.env.example` updated if a new variable is needed.
- R9.5 When `docker compose config -q` runs for `all-in-one`, `server` and `kiosk`, the system shall report no errors.
- R9.6 The `kiosk` profile shall remain startable without the backend, PostgreSQL or Redis.

### R10 Documentation

**User story:** As a developer, I want short instructions in the service folder so that I can build, test and run the backend without asking.

Acceptance criteria:

- R10.1 `services/core-backend/README.md` shall document the prerequisites, build, test (unit and integration, and how to tell them apart), run, single-test commands, `docker build`, the module layout, the architecture rules and the migration rule, with `./mvnw` commands.
- R10.2 `AGENTS.md` shall be updated in the same pull request: the backend commands in the CI section, the single-test example, the project status line and the "Backend architecture" section.
- R10.3 The README shall document how to start the backend in both modes (`all-in-one` and `server`).

### R11 Constraints carried over

**User story:** As the project owner, I want the privacy, security and licensing rules to hold from the first line of code.

Acceptance criteria:

- R11.1 The system shall not store or log frames, face crops, embeddings or per-person data in this feature.
- R11.2 The Java code shall be in English; schema and event field names shall follow `contracts/` when later features introduce them.
- R11.3 The skeleton shall not enable permissive security defaults beyond what R5 allows: no other open endpoint, no CORS wildcard, no debug or developer tooling enabled by default.

### R12 Clean Architecture rules, enforced by tests

**User story:** As an architect, I want the Clean Architecture and DDD rules checked automatically so that no later feature can break them without the build failing.

Acceptance criteria:

- R12.1 The `domain` package of every module shall not depend on Spring, JPA/Jakarta Persistence, Flyway, Jackson, JDBC or any `application` or `infrastructure` package.
- R12.2 The `application` package shall depend only on the `domain` package of its own module and on the published surface of other modules, and not on any `infrastructure` package.
- R12.3 The `infrastructure` package may depend on `application` and `domain`; no other package shall depend on `infrastructure`, except for the Spring wiring.
- R12.4 Access between modules shall go only through each module's published surface, verified together with R2.3.
- R12.5 If a rule is violated, then `./mvnw verify` shall fail with a message that names the offending class.
- R12.6 The rules shall be documented in the README and in `AGENTS.md` ("Backend architecture"): DDD vocabulary (aggregates, value objects, domain events, repository ports), a domain free of frameworks, and repositories declared as interfaces in the domain or application layer and implemented in infrastructure.

### R13 Database independence

**User story:** As an architect, I want the database to be replaceable so that moving from PostgreSQL to SQL Server or another engine does not require changing the domain.

Acceptance criteria:

- R13.1 The skeleton shall contain no database-vendor-specific code outside `infrastructure` and the Flyway configuration.
- R13.2 Flyway shall load migrations from `db/migration/common` (portable SQL) plus `db/migration/<vendor>` (engine-specific), where the vendor comes from configuration (for example `postgresql` or `h2`).
- R13.3 The skeleton shall provide the folders for `common`, `postgresql` and the vendor used by the tests, with a baseline migration and no domain tables.
- R13.4 To support a new engine, the only changes shall be its JDBC driver dependency, a `db/migration/<vendor>` folder and configuration; no change shall be needed in `domain` or `application`. This procedure shall be documented in the README.
- R13.5 The documentation shall state plainly that passing tests on H2 does not prove compatibility with another engine, and that a new engine requires running the migrations and the integration checks against that engine.

## Out of scope

- Authentication and the admin user (KIO-32), and Spring Security configuration beyond what the skeleton needs.
- Entities, aggregates, repositories and the data-access technology choice (KIO-24 onward); campaign tables (KIO-24); rules logic (KIO-25 to KIO-28); reports and aggregates (KIO-33).
- Redis client and the audience event consumer (KIO-23), WebSocket orders to the player (KIO-29).
- Media storage (KIO-31), import and export (KIO-30), the system status endpoint (KIO-34).
- Serving the compiled web app as static files, and the player (KIO-8, KIO-35).
- Transport decision between stations and the server, and publishing Redis on a host port (KIO-9 and the open design decision).

## Open questions (to settle in design.md)

1. Exact Spring Boot and Spring Modulith versions, confirmed against Spring's published releases.
2. Whether the layer rules (R12) use ArchUnit, Spring Modulith's own checks, or both (likely both).
3. How a module exposes its published surface: Modulith named interfaces or a base-package API.
4. Runtime base image, build caching and image size.
5. HTTP port, and whether the management port is separate from the application port.
6. PostgreSQL health check in compose, and whether to add it to the existing `postgres` service.
7. Whether `ci.yml` needs a change (wrapper, Maven caching).
8. Whether the backend needs Redis variables now (proposal: no, since no code uses Redis yet).
9. The data-access technology (JPA or another) is deliberately left to KIO-24.

Accepted risk: H2 can hide engine-specific problems. It is mitigated by R13.5 and R6.7.
