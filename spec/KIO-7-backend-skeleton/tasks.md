# KIO-7 Core backend skeleton: tasks

Implements `design.md` for `requirements.md`. Do the tasks in order and tick each one when done. Commands run from `services/core-backend` unless a task says "repo root". Do not commit, push or open a PR until the user asks. If a Boot 4 or Modulith 2 detail in `design.md` turns out to be different when compiling, fix the design first and tell the user (do not work around it).

## Tasks

- [x] **T1 Maven Wrapper and `pom.xml`** (R1.1–R1.5)
  Generate the wrapper with Maven 3.9.16 (`mvn -N wrapper:wrapper -Dmaven=3.9.16`, plugin 3.3.4) after confirming that distribution exists on Maven Central. Create `pom.xml` as in `design.md` section 3.1: parent Spring Boot 4.1.1, Java 21, coordinates `com.cvretail:core-backend`, Modulith BOM 2.1.1, the compile, runtime and test dependencies listed there (ArchUnit pinned to 1.4.2), `spring-boot-maven-plugin`, and `maven-failsafe-plugin` with the `integration-test` and `verify` goals. Mark `mvnw` executable in git (`git update-index --chmod=+x` once it is added, or `git add --chmod=+x`).
  *Verify:* `./mvnw -v` prints Maven 3.9.16 and Java 21; `./mvnw -B -ntp validate` succeeds.

- [x] **T2 Application class and module packages** (R2.1, R2.2, R2.5, R12.6)
  Create `KioskoApplication` in `com.cvretail.kiosko`. For each of `campaigns`, `rules`, `reports`: a base-package `package-info.java` annotated with `@ApplicationModule(displayName = ...)` and sub-packages `domain`, `application`, `infrastructure`, each with a Javadoc-only `package-info.java` that states what belongs there and the dependency rule. No other classes.
  *Verify:* compiles in T4.

- [x] **T3 Configuration, migrations and requirement wording** (R3.1, R3.4, R4.1–R4.4, R13.1–R13.3)
  Create `src/main/resources/application.yml` as in `design.md` section 3.4, `db/migration/common/V1__baseline.sql` (comment plus `SELECT 1;`), `db/migration/postgresql/README.md` and `db/migration/h2/README.md`, and `src/test/resources/application-test.yml` (H2 in memory, PostgreSQL mode, vendor `h2`). Update the wording of R4.1 in `requirements.md` to say that the connection comes from `DB_URL`, `DB_USER`, `DB_PASSWORD` and `DB_VENDOR`, derived from `POSTGRES_*` by compose, as agreed in `design.md` section 7.
  *Verify:* YAML files parse (`python3 -c "import yaml..."`); the vendor folders and the baseline exist under `src/main/resources`.

- [x] **T4 First compile: Boot 4 and Modulith 2 compatibility** (R1.2, R1.3)
  Run `./mvnw -B -ntp -DskipTests package`. Fix dependency names or annotation packages if Boot 4 differs from the design, correcting `design.md` section 3.1 and 3.5 to match.
  *Verify:* build succeeds and produces `target/core-backend-*.jar`; record the resolved versions of Boot, Modulith, Flyway, H2 and PostgreSQL JDBC (`./mvnw dependency:tree`).

- [x] **T5 Unit tests** (R2.3, R4.3, R6.2, R6.6, R6.8, R12.1–R12.5)
  Create `ModularityTest` (exactly three modules and `verify()`), `ArchitectureRules` (the three rules with `allowEmptyShould(true)`), `ArchitectureTest` (production classes, tests excluded), the fixtures under `src/test/java/com/cvretail/archfixtures/` (a `domain` class using Spring, an `application` class using `infrastructure`, a stray class using `infrastructure`), `ArchitectureRulesDetectViolationsTest` (each rule fails on its fixture and the message names the class), and `ConfigurationTest` (a resolver with only `application.yml` throws naming `DB_URL`). No Spring context in any of them.
  *Verify:* `./mvnw -B -ntp test` runs exactly these four test classes and passes.

- [x] **T6 Integration tests** (R5.1–R5.4, R6.1, R6.3, R6.9)
  Create `KioskoApplicationIT` (context with profile `test`, Flyway applied `V1` from `common`, history holds only the baseline) and `HealthEndpointIT` (MockMvc: `/actuator/health` is 200 with status `UP` and no `components`; `/actuator/env`, `/actuator/info`, `/actuator/beans` are 404).
  *Verify:* `./mvnw -B -ntp verify` runs the two `*IT` classes through Failsafe and passes.

- [x] **T7 Full local verification and negative checks** (R6.4, R6.5, R6.10, R12.5)
  Run `./mvnw -B -ntp test` (only unit) and `./mvnw -B -ntp verify` (unit, package, integration) on a machine without any service started. Then temporarily (a) add a `domain` class that imports Spring and confirm the build fails naming it, (b) break an integration test and confirm `verify` fails while `test` passes, (c) add a class in `campaigns.infrastructure` and use it from `rules` to confirm Modulith fails. Remove all temporary changes. Confirm single-test commands for a unit test and an integration test and use the working flags in the README.
  *Verify:* every negative check fails as expected and `git status` shows no temporary files.

- [x] **T8 Dockerfile and image** (R8.1–R8.5)
  Create `Dockerfile` and `.dockerignore` as in `design.md` section 3.7. Requires a running Docker daemon: if it is not running, ask the user to start Docker Desktop. Build `docker build -t kiosko-core-backend .`, check the jar glob picks a single jar, run `docker run --rm --entrypoint id kiosko-core-backend -u` (not `0`), and check the image contains no `.env` or caches. Build once for `linux/amd64` under emulation to confirm it builds.
  *Verify:* build succeeds on arm64 (native) and amd64 (emulated), non-root confirmed; report anything not run.

- [x] **T9 Compose** (R9.1, R9.4–R9.6; repo root)
  Add the `healthcheck` to `postgres` and the `core-backend` service to `docker-compose.yml` as in `design.md` section 3.8, and update the comment at the top of the file. Do not change `.env.example` unless a new variable turns out to be needed.
  *Verify:* `cp .env.example .env`, then `docker compose --profile all-in-one|server|kiosk config -q` pass; `config --services` shows `kiosk` → `vision-service` only and `server` → `core-backend postgres redis`; remove the `.env` copy afterwards.

- [x] **T10 Compose smoke test on real PostgreSQL** (R5.3, R6.7, R9.2)
  With Docker running: `docker compose --profile server up --build -d`; wait for PostgreSQL healthy and the backend healthy; `curl -fsS localhost:8080/actuator/health` returns `{"status":"UP"}`; inspect the Flyway history on PostgreSQL (`docker compose exec postgres psql ...`) and confirm `V1`. Stop PostgreSQL and confirm the endpoint reports `DOWN` and the backend container becomes unhealthy; restart it. Then `down -v`, run `--profile all-in-one up --build -d` and confirm PostgreSQL, Redis, `core-backend` and `vision-service` behave as expected, and `--profile kiosk up --build` starts only `vision-service`. Clean everything and delete the local `.env`.
  *Verify:* record each observation in the "Verification results" section of this file.

- [x] **T11 CI** (R7.1–R7.3)
  In `.github/workflows/ci.yml`, change the `Core backend` job command from `mvn -B -ntp verify` to `./mvnw -B -ntp verify`. Keep detection, Java 21 and the Maven cache. Validate the YAML.
  *Verify:* YAML parses; after the PR is opened, `Core backend` runs for real (not skipped) and `CI success` is green.

- [x] **T12 Documentation** (R10.1–R10.3, R12.6, R13.4, R13.5, R3.5)
  Write `services/core-backend/README.md` as in `design.md` section 3.10: prerequisites, build, unit versus integration tests and how to run each, single-test commands, run with the four `DB_*` variables, `docker build`, both compose modes, module layout and the published-surface rule, architecture rules, how to add a database engine, the H2 caveat and the migration rule. Update `AGENTS.md`: project status line, the backend commands in the CI section, and the "Backend architecture" section (published surface = module base package).
  *Verify:* every command in the README was run in T5 to T10 or is labeled as not run.

- [x] **T13 Dependency, license and privacy review** (R1.4, R11.1–R11.3)
  Confirm the license of `flyway-core` and `flyway-database-postgresql` 12.4.0 and that the Community edition supports PostgreSQL and H2 (replace the "not confirmed" row in `design.md` section 4). Review `./mvnw dependency:tree` for any AGPL or non-commercial license. Grep `src/main` for `CorsRegistry`, `allowedOrigins`, `spring-boot-devtools`, `logging`, `Frame`, `Image` and endpoint mappings other than Actuator health (expected: none).
  *Verify:* license table in `design.md` fully confirmed or corrected; grep results reported.

- [x] **T14 Final pass**
  From the repo root: `git status` shows only intended files (spec documents, `AGENTS.md`, `services/core-backend/**`, `docker-compose.yml`, `.github/workflows/ci.yml`); no `target/`, `.env` or caches are tracked; `mvnw` has the executable bit in git; re-run `./mvnw -B -ntp verify` once; tick every task here; re-read `requirements.md` and confirm each requirement has a covering task and a result. Report what was verified and what was not. Wait for the user to ask for the commit, push and PR (`feature/KIO-7-backend-skeleton` → `develop`).

## Optional, only if the user asks

- Move Trello card KIO-7 to "En proceso" now and to "En review" when the PR opens.
- Add a note to KIO-24 saying domain code must follow the layers in `AGENTS.md` ("Backend architecture").

## Requirement coverage

| Requirement | Tasks |
|---|---|
| R1 Maven project | T1, T4, T13 |
| R2 Modules | T2, T5, T7 |
| R3 Migrations | T3, T6, T12 |
| R4 Configuration | T3, T5 |
| R5 Health | T6, T10 |
| R6 Tests | T5, T6, T7, T10 |
| R7 CI | T11 |
| R8 Container | T8 |
| R9 Compose | T9, T10 |
| R10 Documentation | T12 |
| R11 Constraints | T13 |
| R12 Architecture rules | T2, T5, T7, T12 |
| R13 Database independence | T3, T12 |

## Verification results (2026-10-07)

- **Versions resolved:** Spring Boot 4.1.1, Spring Modulith 2.1.1, Flyway 12.4.0, H2 2.4.240, PostgreSQL JDBC 42.7.13, JUnit 6.0.3, ArchUnit 1.4.2, Maven 3.9.16 (wrapper), Java 21.0.11. Boot 4 and Modulith 2 compile and run together; the starter names and the MockMvc package (`org.springframework.boot.webmvc.test.autoconfigure`) in `design.md` were correct.
- **Tests:** `./mvnw test` runs 12 unit tests (4 classes); `./mvnw verify` also runs 3 integration tests (2 classes); all pass, no Docker needed.
- **Negative checks:** a `domain` class annotated with `@Service` fails `ArchitectureTest` naming `TempBadDomain`; `rules` using a `campaigns.infrastructure` type fails `ModularityTest` and `ArchitectureTest` naming the field; a broken `*IT` fails `verify` but not `test`. All temporary files removed.
- **Single test commands:** unit `./mvnw -Dtest=ModularityTest test`; integration `./mvnw verify -Dit.test=HealthEndpointIT -Dtest=NoMatch -Dsurefire.failIfNoSpecifiedTests=false` (the `-Dsurefire.skip` variant also ran the unit tests, so it was discarded).
- **Correction found in T6:** Flyway also records an unversioned schema-creation row, so the integration test counts only versioned migrations.
- **Image:** `docker build --check` clean; builds natively on linux/arm64 (140 MB) and for linux/amd64 (emulated build only; the amd64 image was not run); runs as uid 999; one jar in `/app`; no `.env`.
- **Compose smoke test on real PostgreSQL 16 (profile `server`):** the backend waited for PostgreSQL, applied `V1` (history row `1 | baseline | success`), `GET /actuator/health` returned `{"status":"UP"}` (plus the `groups` list `liveness`, `readiness`, no details); `/actuator/env`, `/info`, `/beans` returned 404.
- **Finding and fix (R5.3):** with PostgreSQL stopped the endpoint answered `DOWN` (503) only after 30 s (Hikari default). Added `spring.datasource.hikari.connection-timeout: 5000`; after rebuilding, `DOWN` (503) arrives in 5 s, the container turned `unhealthy` after about 80 s (five failed checks, 15 s interval) and the endpoint returned 200 again once PostgreSQL restarted.
- **Profiles:** `config -q` passes for `all-in-one`, `server` and `kiosk`; `server` lists `core-backend postgres redis`; `kiosk` lists `vision-service` only. `--profile all-in-one up` ran PostgreSQL, Redis, `core-backend` (healthy) and `vision-service` (prints version, exit 0); `--profile kiosk up` started only `vision-service`. Everything was brought down with volumes and the local `.env` removed.
- **Licenses (70 runtime artifacts):** no AGPL, no non-commercial. Flyway 12.4.0 is Apache-2.0 and works on PostgreSQL and H2 in the Community modules. Weak-copyleft libraries to be aware of: logback 1.5.38 (EPL-2.0 or LGPL-2.1) and `jakarta.annotation-api` 3.0.0 (EPL-2.0 or GPL-2.0 with Classpath Exception); H2 is test-only.
- **Privacy and security grep** on `src/main`: no controllers or request mappings, no CORS configuration, no devtools, no logging statements, no frame, image or embedding code. Only `health` is exposed, with `show-details: never`.
- **Not verified:** the CI run on GitHub (it happens when the PR is opened), the amd64 image at runtime, the arm64 image on a real Raspberry Pi, and Flyway on any engine other than PostgreSQL and H2.
