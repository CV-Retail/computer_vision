# core-backend

Core backend of the smart kiosk: campaigns, rules engine and reports. It is a Spring Boot application (KIO-7) with Spring Modulith modules, Clean Architecture layers enforced by tests, database-independent Flyway migrations and a health endpoint. The campaign domain (aggregate, value objects, status life cycle, events and repository port) arrived with KIO-24; it has no persistence yet.

Design: [`spec/KIO-7-backend-skeleton/`](../../spec/KIO-7-backend-skeleton/), [`spec/KIO-24-campaign-domain/`](../../spec/KIO-24-campaign-domain/) and [`docs/kiosko-arquitectura-mvp.md`](../../docs/kiosko-arquitectura-mvp.md).

Stack: Java 21, Spring Boot 4.1.1, Spring Modulith 2.1.1, Flyway, PostgreSQL (runtime), H2 and Cucumber (tests only).

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

The Cucumber scenarios of the domain run as part of `./mvnw test` (see [Behavior-driven specification](#behavior-driven-specification)).

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
- **Server apart from the stations:** `docker compose --profile server up --build` on the server (PostgreSQL, the event bus, this backend and the web). The backend never runs under the `kiosk` profile.

Compose builds `DB_URL`, `DB_USER`, `DB_PASSWORD` and `DB_VENDOR` from the `POSTGRES_*` variables and waits for PostgreSQL to be healthy. **The backend does not publish a port on the host:** it is reachable only inside the Compose network, through the `web` service, which proxies `/api` and `/ws` to it (the REST endpoints under `/api` arrive with KIO-58 onward; today an unknown `/api/...` returns the backend's 404). `/actuator` is not proxied.

To check its health from the host while it runs under Compose:

```bash
docker compose --profile server exec core-backend curl -fsS http://localhost:8080/actuator/health
```

The `curl localhost:8080/actuator/health` shown in "Run" applies when you start the backend yourself with `./mvnw spring-boot:run`, outside Compose.

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
- The published surface does not depend on `domain`, `application`, `infrastructure` or Spring.
- Inside `domain`, the building-block packages (`entity`, `event`, `exception`, `repository`, `service`, `valueobject`) follow the dependencies in [Campaign domain](#campaign-domain).

`ArchitectureRulesDetectViolationsTest` runs the same rules against deliberately bad classes in `src/test/java/com/cvretail/archfixtures` to prove they fail.

## Campaign domain

Package `com.cvretail.kiosko.campaigns` (spec: `spec/KIO-24-campaign-domain/`). The `domain` package is organized by building block, and the tests enforce which one may use which:

```
campaigns/
├── CampaignView, CampaignCatalog   published surface: read-only view and query port for other modules
├── application/CampaignViews       maps a Campaign to its CampaignView
└── domain/
    ├── entity/       Campaign (aggregate root)
    ├── event/        CampaignCreated, CampaignScheduled, CampaignActivated, CampaignPaused, CampaignFinished
    ├── exception/    CampaignViolationException, Violation, ViolationCodes
    ├── repository/   CampaignRepository (port)
    ├── service/      CampaignStatusPolicy, CampaignInvariants
    └── valueobject/  identifiers, CampaignName, Playlist, Schedule, Scope, enums, CampaignSnapshot
```

Allowed dependencies inside `domain`: `exception` uses nothing; `valueobject` uses `exception`; `event` uses `valueobject`; `service` uses `valueobject` and `exception`; `entity` uses all of them but `repository`; `repository` uses `entity` and `valueobject`.

**Status life cycle** (a finished campaign accepts no transition and no change):

| Transition | Allowed from | Result | Needs |
|---|---|---|---|
| `schedule()` | `DRAFT` | `SCHEDULED` | complete, with a start date |
| `activate()` | `DRAFT`, `SCHEDULED`, `PAUSED` | `ACTIVE` | complete |
| `pause()` | `SCHEDULED`, `ACTIVE` | `PAUSED` | nothing |
| `finish()` | `SCHEDULED`, `ACTIVE`, `PAUSED` | `FINISHED` | nothing |

The aggregate does not read the clock: moving from `SCHEDULED` to `ACTIVE` by date is a later use case (KIO-54).

**Rules by type**

| Field | `REGULAR` | `DEFAULT` |
|---|---|---|
| priority | `HIGH`, `MEDIUM` (default) or `LOW` | not allowed |
| audience rule | optional in a draft, required to schedule or activate | not allowed |
| minimum time on screen | 1 to 3600 s, default 8 s | not allowed |
| manual vetoes | a set without duplicates | not allowed |
| schedule | dates, days, time slot | always on, not changeable |

A campaign is **complete** when it has a non-empty playlist and, if regular, an audience rule (and a start date to be scheduled). `DRAFT` and `PAUSED` accept incomplete states; `SCHEDULED` and `ACTIVE` reject any change that would leave the campaign incomplete, and a rejected change leaves the campaign untouched. Other bounds: name 1 to 100 characters, playlist up to 50 items, item duration 1 to 3600 s.

**Errors.** A broken rule throws `CampaignViolationException`, which carries a list of `Violation(field, code, message)`. The codes (`name.blank`, `playlist.empty`, `status.transition_not_allowed`, ...) are stable and listed in `ViolationCodes`.

**Events.** The aggregate records its status events and hands them over with `pullEvents()`. Nothing publishes them yet.

**Published surface.** Other modules (for example `rules`) read campaigns through `CampaignView` and `CampaignCatalog`; the adapter that implements the catalog comes with persistence (KIO-49).

### Glossary

| Spanish business term | English identifier |
|---|---|
| campaña | `Campaign` |
| regla de audiencia | audience rule (`AudienceRuleId`) |
| veto | veto (`VetoId`, `manualVetoes`) |
| medio | media (`MediaRef`) |
| playlist | `Playlist` |
| programación | `Schedule` |
| alcance | `Scope` |
| borrador, programada, activa, pausada, finalizada | `DRAFT`, `SCHEDULED`, `ACTIVE`, `PAUSED`, `FINISHED` |
| prioridad: alta, media, baja | `Priority`: `HIGH`, `MEDIUM`, `LOW` |

## Behavior-driven specification

The business behavior of the campaign is written as Gherkin scenarios in English and run with Cucumber through JUnit:

- Scenarios: `src/test/resources/features/campaigns/*.feature`.
- Step definitions: `src/test/java/com/cvretail/kiosko/campaigns/bdd/` (one steps class per feature area; `CampaignWorld` holds the state of one scenario).
- Runner: `CampaignDomainBehaviourTest`, a `*Test` class, so `./mvnw test` runs the scenarios with the unit tests. No Spring context or database is involved.
- Technical details (value object bounds, repository contract) are plain unit tests, not scenarios.

```bash
./mvnw -B -ntp test                                                        # everything
./mvnw -Dtest=CampaignDomainBehaviourTest test                             # only the scenarios
./mvnw -Dtest=CampaignDomainBehaviourTest -Dcucumber.filter.tags="@R11" test   # one tag
./mvnw -Dtest=CampaignDomainBehaviourTest \
  -Dcucumber.features=src/test/resources/features/campaigns/playlist.feature test   # one feature
```

Every scenario carries tags with the requirement ids it verifies (`@R11`, `@R11.2`) from `spec/KIO-24-campaign-domain/requirements.md`, so `grep -r "@R12" src/test/resources/features` shows what covers a requirement. The HTML report is written to `target/cucumber-reports/campaigns.html` and is not committed.

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
- Domain tables arrive with the persistence adapter (KIO-49); the baseline `V1` creates none.

## Rules

- Never log or store frames, face crops, embeddings or per-person data.
- Do not add AGPL or non-commercial dependencies.
