# KIO-7 Core backend skeleton: design

Implements `requirements.md` (R1–R13). Scope is the skeleton only: Maven project and wrapper, Spring Modulith modules with Clean Architecture layers enforced by tests, database-independent Flyway migrations, a health-only Actuator endpoint, tests, container, compose service, CI activation and docs. No domain logic.

## 1. Overview

A Spring Boot application, `com.cvretail:core-backend`, base package `com.cvretail.kiosko`. It has three Spring Modulith modules (`campaigns`, `rules`, `reports`), each with empty `domain`, `application` and `infrastructure` packages. The only runtime behavior is: connect to a database, apply Flyway migrations, expose `/actuator/health`. Everything else is structure and tests that keep the structure honest.

```
services/core-backend/
├── pom.xml
├── mvnw, mvnw.cmd, .mvn/wrapper/maven-wrapper.properties
├── Dockerfile
├── .dockerignore
├── README.md
├── src/main/java/com/cvretail/kiosko/
│   ├── KioskoApplication.java
│   ├── campaigns/
│   │   ├── package-info.java            @ApplicationModule (published surface = this package)
│   │   ├── domain/package-info.java
│   │   ├── application/package-info.java
│   │   └── infrastructure/package-info.java
│   ├── rules/        (same shape)
│   └── reports/      (same shape)
├── src/main/resources/
│   ├── application.yml
│   └── db/migration/
│       ├── common/V1__baseline.sql
│       ├── postgresql/README.md
│       └── h2/README.md
└── src/test/
    ├── java/com/cvretail/kiosko/
    │   ├── KioskoApplicationIT.java          integration: context + migrations on H2
    │   ├── ModularityTest.java              unit: Modulith verification, exactly 3 modules
    │   ├── ArchitectureTest.java            unit: ArchUnit rules on production code
    │   ├── ArchitectureRulesDetectViolationsTest.java   unit: rules fail on bad fixtures
    │   ├── HealthEndpointIT.java             integration: MockMvc
    │   └── ConfigurationTest.java           unit: missing variable names the variable
    ├── java/com/cvretail/archfixtures/...    deliberately bad classes, outside the app package
    └── resources/application-test.yml
```

Files changed outside the service folder: `docker-compose.yml`, `.github/workflows/ci.yml`, `AGENTS.md`, `README.md` only if needed. `.gitignore` already ignores `target/`.

## 2. Decisions on the open questions

| Question | Decision | Why |
|---|---|---|
| Spring versions | Spring Boot **4.1.1** (the default of `start.spring.io`), Spring Modulith **2.1.1** (latest stable on Maven Central) | Verified on 2026-10-07: Boot 4.1.1 is the newest non-snapshot, non-milestone release; Modulith 2.1.1 is the newest stable (2.2.0 is a milestone). Compatibility is proved by the build in T4, not assumed |
| Dependency versions | Not pinned except ArchUnit | Boot manages Flyway 12.4.0, H2 2.4.240, PostgreSQL JDBC 42.7.13, JUnit 6.0.3. ArchUnit is not managed by Boot; Modulith depends on ArchUnit **1.4.2**, so the test dependency is pinned to the same version |
| Layer rules tooling | **Both**: Modulith `verify()` for modules and cycles; **ArchUnit** for the layer rules | Modulith does not know about `domain/application/infrastructure` layers inside a module |
| ArchUnit integration | Plain `ClassFileImporter` + `rule.check(...)` inside normal JUnit tests; do **not** use `archunit-junit5` | Avoids depending on ArchUnit's JUnit 5 engine working with JUnit 6 |
| Published surface of a module | The module's **base package** (Modulith default). `domain`, `application`, `infrastructure` are internal. No `@NamedInterface` for now | Simplest rule and matches R2.5. A type another module needs (for example a domain event) is placed in the base package; revisit if that becomes awkward |
| Test database | H2 in memory, PostgreSQL compatibility mode, vendor `h2` | User decision; mitigations in section 8 |
| Database configuration | Vendor-neutral variables `DB_URL`, `DB_USER`, `DB_PASSWORD`, `DB_VENDOR`. The compose file builds them from the existing `POSTGRES_*` variables | A JDBC URL is engine-specific. One `DB_URL` plus `DB_VENDOR` means switching engine is configuration only (R13.4). **Deviates slightly from R4.1** (which listed host/port/name): the URL carries them. No new `.env` variable is needed |
| HTTP port | One port, **8080**, no separate management port | The health test and healthcheck stay simple. Only `health` is exposed, with `show-details: never` |
| Maven Wrapper | Yes, generated with `maven-wrapper-plugin` 3.3.4 and pinned to the Maven version installed here (3.9.16, availability on Central confirmed in T1) | Same Maven everywhere; CI and agents run `./mvnw` |
| Runtime image | `eclipse-temurin:21-jre`, multi-arch, with `curl` installed for the healthcheck | The JRE image has no HTTP client. The alternative (a Java healthcheck program) costs a JVM start every few seconds |
| Image build | Multi-stage: `eclipse-temurin:21-jdk` builds with `./mvnw -DskipTests package` using a BuildKit cache mount for `~/.m2`; tests run in CI, not in the image build | Faster builds and no tests inside `docker build` |
| `ci.yml` | `Core backend` job switches from `mvn` to `./mvnw` | Same command for everyone; the wrapper file must be committed with the executable bit |
| Redis variables | None | No code uses Redis yet (KIO-23) |
| Postgres health check | Added to the existing `postgres` service (`pg_isready`) | `depends_on: condition: service_healthy` needs it. Small, justified change to the server-side service |
| Data-access technology | Not chosen | KIO-24 decides; the skeleton uses `spring-boot-starter-jdbc` only for the `DataSource` that Flyway and the health check need |

## 3. Components

### 3.1 `pom.xml` (R1)

- Parent `org.springframework.boot:spring-boot-starter-parent:4.1.1`; `java.version` 21; coordinates `com.cvretail:core-backend:0.1.0-SNAPSHOT`.
- `dependencyManagement`: import `org.springframework.modulith:spring-modulith-bom:2.1.1`.
- Dependencies (names are the Boot 4 modular starters, which exist on Maven Central for 4.1.1):
  - compile: `spring-boot-starter-webmvc`, `spring-boot-starter-actuator`, `spring-boot-starter-jdbc`, `spring-boot-starter-flyway`, `spring-modulith-starter-core`.
  - runtime: `org.flywaydb:flyway-database-postgresql`, `org.postgresql:postgresql`.
  - test: `spring-boot-starter-test`, `spring-boot-starter-webmvc-test`, `spring-modulith-starter-test`, `com.h2database:h2`, `com.tngtech.archunit:archunit:1.4.2`.
- Plugins: `spring-boot-maven-plugin` (repackage); Surefire from the parent for **unit tests**, and `maven-failsafe-plugin` (version managed by the Spring Boot parent) declared with the `integration-test` and `verify` goals for **integration tests**. Surefire's defaults pick up `*Test`; Failsafe's defaults pick up `*IT`. Do not name tests `*Tests`: Surefire would run them as unit tests. `./mvnw test` runs only unit tests; `./mvnw verify` runs compile, unit tests, packaging and integration tests.
- Exact artifact names and test annotations in Boot 4 (for example the MockMvc auto-configuration package) are confirmed by compiling in T4; if a name differs, this section is corrected, not worked around.

### 3.2 Modules and layers (R2, R12)

- `KioskoApplication` in `com.cvretail.kiosko` with `@SpringBootApplication`.
- `campaigns/package-info.java`, `rules/package-info.java`, `reports/package-info.java` annotated with `@ApplicationModule(displayName = "...")`. The annotation makes javac emit `package-info.class`, so Modulith detects each module even though it has no other class.
- `domain`, `application` and `infrastructure` sub-packages each have a `package-info.java` with Javadoc describing what belongs there and the dependency rule. Without an annotation these produce no class file: they are documentation only.
- Published surface: the module base package. Nothing else is public across modules.

### 3.3 Architecture rules (R12)

`ArchitectureRules` (test class) defines the rules as `ArchRule` constants with `allowEmptyShould(true)` because the layers are empty today:

1. **Domain is framework-free:** classes in `..domain..` shall not depend on `..application..`, `..infrastructure..`, `org.springframework..`, `jakarta..`, `javax..`, `org.flywaydb..`, `tools.jackson..`, `com.fasterxml.jackson..`, `java.sql..` or `javax.sql..`.
2. **Application does not know infrastructure:** classes in `..application..` shall not depend on `..infrastructure..`.
3. **Infrastructure is a leaf:** classes outside `..infrastructure..` and outside the root package `com.cvretail.kiosko` itself (the Spring wiring) shall not depend on `..infrastructure..`.
4. Cross-module access and cycles are checked by Modulith in `ModularityTest`, not duplicated.

Two tests use these rules:

- `ArchitectureTest` imports the production classes (`DoNotIncludeTests`) and checks every rule. It is vacuous today and becomes real as code lands.
- `ArchitectureRulesDetectViolationsTest` imports the fixtures under `com.cvretail.archfixtures` (a package outside the application package, so Modulith ignores it) and asserts that each rule **fails** on a deliberately bad class (a `domain` class using Spring, an `application` class using `infrastructure`, a stray class using `infrastructure`) and that the error message names the offending class (R12.5). This proves the rules work even while the real layers are empty.

### 3.4 Database and migrations (R3, R13)

`application.yml`:

```yaml
spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USER}
    password: ${DB_PASSWORD}
    hikari:
      connection-timeout: 5000
  flyway:
    locations: classpath:db/migration/common,classpath:db/migration/${DB_VENDOR}
server:
  port: 8080
management:
  endpoints:
    web:
      exposure:
        include: health
  endpoint:
    health:
      show-details: never
```

- A missing variable makes Spring fail with "Could not resolve placeholder 'DB_URL'", which names the variable (R4.3). `ConfigurationTest` proves it with a property resolver that has only `application.yml` loaded, so it does not depend on the developer's environment.
- `spring.datasource.hikari.connection-timeout: 5000` was added after the compose smoke test: with Hikari's default (30 s) the health endpoint took 30 s to answer `DOWN` when PostgreSQL was unreachable.
- `db/migration/common/V1__baseline.sql`: a comment explaining the baseline and `SELECT 1;`. It creates no table (R3.4, R13.3) and runs on H2 and PostgreSQL.
- `db/migration/postgresql/README.md` and `db/migration/h2/README.md`: documents that engine-specific migrations go here and that version numbers are shared across locations, so they must not repeat a version used in `common`. The files exist so the locations resolve and Flyway does not warn about a missing folder; non-SQL files are ignored by Flyway.
- Adding SQL Server later: add `flyway-sqlserver` and the JDBC driver, create `db/migration/sqlserver/`, set `DB_VENDOR=sqlserver` and `DB_URL`. No change in `domain` or `application` (R13.4). The README says the new engine must run the migrations and the integration checks (R13.5).
- `application-test.yml` (profile `test`): `DB_*` equivalents set directly: `jdbc:h2:mem:kiosko;MODE=PostgreSQL;DB_CLOSE_DELAY=-1`, user `sa`, empty password, vendor `h2`.

### 3.5 Health endpoint (R5)

- Only `health` is exposed, no details. The database health indicator comes from the `DataSource`, so an unreachable database makes the endpoint report `DOWN` with HTTP 503.
- `HealthEndpointIT` (MockMvc): `GET /actuator/health` returns 200 and `{"status":"UP"}` with no `components` field; `GET /actuator/env`, `/actuator/info` and `/actuator/beans` return 404 (R5.2).
- R5.3 (database down) is Spring's built-in behavior and cannot be tested on an in-memory H2 without contrivance; it is verified by hand in the compose smoke test (stop PostgreSQL, observe `DOWN` and the container turning unhealthy).

### 3.6 Tests (R6)

| Test class | Type | Covers |
|---|---|---|
| `ModularityTest` | Unit | `ApplicationModules.of(KioskoApplication.class)` has exactly `campaigns`, `rules`, `reports`; `verify()` passes (R2.1, R2.3, R6.2). Static analysis, no Spring context |
| `ArchitectureTest`, `ArchitectureRulesDetectViolationsTest` | Unit | R12, R6.6. ArchUnit only |
| `ConfigurationTest` | Unit | R4.3. No Spring context |
| `KioskoApplicationIT` | Integration | Context starts with profile `test`; Flyway applied `V1` from `common`; the only versioned migration in the history table is the baseline `V1` (Flyway also records an unversioned schema-creation row) (R6.1) |
| `HealthEndpointIT` | Integration | R5, R6.3. Spring context with MockMvc |

Unit tests (`*Test`) start no Spring context, no database and no network, and run in milliseconds with `./mvnw test`. Integration tests (`*IT`) may start the Spring context with the embedded H2 database and run in the `integration-test` phase through Failsafe (R6.8 to R6.10). Future domain and application logic is covered by unit tests without Spring; keep integration tests few.

`./mvnw verify` runs both kinds on any machine with Java 21, no Docker (R6.4, R6.10).

### 3.7 Container image (R8)

```dockerfile
FROM eclipse-temurin:21-jdk AS build
WORKDIR /build
COPY .mvn .mvn
COPY mvnw pom.xml ./
COPY src src
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -ntp -DskipTests package

FROM eclipse-temurin:21-jre
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --no-create-home kiosko
WORKDIR /app
COPY --from=build /build/target/core-backend-*.jar app.jar
USER kiosko
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=3s --start-period=40s --retries=5 \
  CMD curl -fsS http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

- `.dockerignore`: `target/`, `.env*`, `.idea/`, `*.log`, `.git`.
- The jar glob must match a single jar (the plain jar is replaced by the repackaged one); the Dockerfile is adjusted if two jars appear.
- Both base images are multi-arch (amd64 and arm64). This host is arm64, so the native build is verified; amd64 is built under emulation as in KIO-6.
- Secrets never enter the image: nothing is copied except sources and the wrapper.

### 3.8 Compose (R9)

Added to `docker-compose.yml` and a small addition to `postgres`:

```yaml
  postgres:
    ...
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${POSTGRES_USER} -d ${POSTGRES_DB}"]
      interval: 5s
      timeout: 3s
      retries: 10

  core-backend:
    build: ./services/core-backend
    profiles: [all-in-one, server]
    restart: unless-stopped
    depends_on:
      postgres:
        condition: service_healthy
    environment:
      DB_VENDOR: postgresql
      DB_URL: jdbc:postgresql://postgres:5432/${POSTGRES_DB}
      DB_USER: ${POSTGRES_USER}
      DB_PASSWORD: ${POSTGRES_PASSWORD}
    ports:
      - "8080:8080"
```

- Never under `kiosk`; `docker compose --profile kiosk config --services` still lists only `vision-service` (R9.1, R9.6).
- `.env.example` needs no change (R9.4); the file comment on the profiles is updated.
- `ci.yml`'s compose job already loops over the three profiles (R9.5, R7.3).

### 3.9 CI (R7)

In `.github/workflows/ci.yml`, the `Core backend` job step changes from `mvn -B -ntp verify` to `./mvnw -B -ntp verify`. Detection, Java 21 setup and the Maven cache stay. Because `services/core-backend/pom.xml` now exists, the job runs for real.

### 3.10 Documentation (R10, R12.6)

- `services/core-backend/README.md`: prerequisites (Java 21), `./mvnw -B -ntp verify`, single-test commands for a unit test (`./mvnw -Dtest=ModularityTest test`) and an integration test (`./mvnw verify -Dit.test=HealthEndpointIT -Dtest=NoMatch -Dsurefire.failIfNoSpecifiedTests=false`, confirmed in T7), run with the four `DB_*` variables, `docker build`, both compose modes, module layout and the published-surface rule, the architecture rules, how to add an engine, the H2 caveat and "never edit an applied migration".
- `AGENTS.md`: replace the Backend line in the CI section with `./mvnw -B -ntp verify` and the single-test example; update the project status line; the "Backend architecture" section already exists and is adjusted to the published-surface decision.

## 4. Dependencies and licenses (R1.4)

| Artifact | Scope | License | Status |
|---|---|---|---|
| Spring Boot starters, Spring Modulith | compile | Apache-2.0 | Confirmed from the POMs of `spring-boot-starter-parent` 4.1.1 and `spring-modulith-core` 2.1.1 |
| PostgreSQL JDBC | runtime | BSD-2-Clause | Confirmed from its POM |
| Flyway (`flyway-core`, `flyway-database-postgresql`) 12.4.0 | runtime | Apache-2.0 | Confirmed from the POM (T13) and by use: migrations ran on real PostgreSQL 16 (compose) and on H2 (tests) with the Community modules, without a paid edition |
| H2 | test only | MPL-2.0 / EPL-1.0 | Confirmed from its POM; not shipped in the image |
| ArchUnit | test only | Apache-2.0 | Confirmed from its POM |
| Rest of the runtime classpath (70 artifacts) | runtime | 62 Apache-2.0, 3 MIT, 2 BSD-style, 3 weak-copyleft libraries | Checked in T13 from published POMs. No AGPL, GPL without exception, or non-commercial license. Weak-copyleft: `logback-classic` and `logback-core` 1.5.38 (EPL-2.0 or LGPL-2.1, Spring Boot's default logging, used as unmodified libraries), `jakarta.annotation-api` 3.0.0 (EPL-2.0 or GPL-2.0 with Classpath Exception). `HdrHistogram` is BSD-2-Clause / CC0 |

## 5. Verification strategy

In `services/core-backend`:

- `./mvnw -B -ntp test` runs only the four unit classes; `./mvnw -B -ntp verify` also runs the two `*IT` classes and packages. A deliberately broken integration test fails `verify` but not `test`.
- `./mvnw -Dtest=ModularityTest test` runs one unit test; the single-IT command from the README runs one integration test.
- Temporarily add a violating class (for example a `domain` class importing Spring) and confirm the build fails naming the class, then remove it.
- `docker build -t kiosko-core-backend .` succeeds; `docker run --rm --entrypoint id kiosko-core-backend -u` is not `0`.

From the repo root, with `.env` copied from `.env.example`:

- `docker compose --profile all-in-one|server|kiosk config -q` pass; `kiosk` still lists only `vision-service`.
- `docker compose --profile server up --build -d`: PostgreSQL turns healthy, the backend applies the migration and reports healthy; `curl localhost:8080/actuator/health` returns `{"status":"UP"}`; the Flyway history table in PostgreSQL contains `V1` (R6.7).
- Stop PostgreSQL and observe the health endpoint go `DOWN` and the backend container become unhealthy (R5.3). Bring everything down and remove the `.env` copy.
- `--profile all-in-one up` starts PostgreSQL, Redis, backend and the vision service.
- On GitHub, the PR shows `Core backend` running, not skipped, and `CI success` green.

## 6. Requirement coverage

| Requirement | Covered by |
|---|---|
| R1 Maven project | 3.1, section 4 |
| R2 Modules | 3.2, 3.6 |
| R3 Migrations | 3.4 |
| R4 Configuration | 3.4, 3.6 (`ConfigurationTest`) |
| R5 Health | 3.4, 3.5 |
| R6 Tests | 3.6, section 5 |
| R7 CI | 3.9 |
| R8 Container | 3.7 |
| R9 Compose | 3.8 |
| R10 Documentation | 3.10 |
| R11 Constraints | 3.4, 3.5 (no other endpoint, no CORS config, no dev tools), section 4 |
| R12 Architecture rules | 3.2, 3.3 |
| R13 Database independence | 3.4, 3.1 (vendor drivers only), 3.10 |

## 7. Differences from the requirements

- **R4.1**: connection settings arrive as `DB_URL`, `DB_USER`, `DB_PASSWORD`, `DB_VENDOR` instead of host/port/name; compose derives them from `POSTGRES_*`. Requirement text should be updated to match when this design is approved.
- **R9.2**: adding a health check to the `postgres` service is the only change to an existing service.

## 8. Risks and open points

- **H2 hides engine differences** (accepted by the team). Mitigations: portable SQL in `common`, vendor folders for the rest, the PostgreSQL start in the compose check (R6.7), and the documented caveat (R13.5).
- **Boot 4 is new**: starter names and test annotation packages changed from Boot 3. They are confirmed by compiling; any difference is fixed in the design, not hidden.
- **Vacuous ArchUnit rules today**: covered by the fixture tests that prove the rules fail on bad code.
- **Flyway license and edition for the chosen engines** is the one dependency fact still to confirm (T11).
- **Integration tests are slower** (Spring context): keep them few and push logic into unit tests.
- **Docker build needs network** (Maven Central) and the first build is slow.
- **Wrapper executable bit** must be committed (`git update-index --chmod=+x mvnw`), or CI fails with permission denied.
- **Published surface rule** (base package) may need `@NamedInterface` later if modules share many types.
