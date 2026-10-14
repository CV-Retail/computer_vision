# KIO-24 Campaign domain: design

Implements `requirements.md` (R1–R21). Scope: the campaign aggregate, its value objects, status transitions, recorded domain events, domain errors, the repository port, the published read-only view of the `campaigns` module, and the Cucumber scenarios, unit tests and architecture rules that prove them. No persistence, no Spring, no runtime dependency, no migration, no change to `contracts/`.

## 1. Overview

```
services/core-backend/src/main/java/com/cvretail/kiosko/campaigns/
├── CampaignView.java                      published surface: immutable read model (R15)
├── CampaignCatalog.java                   published surface: read-only query port (R15)
├── package-info.java                      (exists) @ApplicationModule
├── domain/
│   ├── entity/
│   │   └── Campaign.java                  aggregate root: behavior, transitions, recorded events
│   ├── event/
│   │   ├── CampaignEvent.java             sealed interface
│   │   └── CampaignCreated, CampaignScheduled, CampaignActivated, CampaignPaused, CampaignFinished
│   ├── exception/
│   │   ├── Violation.java                 field + code + message
│   │   └── CampaignViolationException.java
│   ├── repository/
│   │   └── CampaignRepository.java        port
│   ├── service/
│   │   ├── CampaignStatusPolicy.java      which transition is allowed from which status
│   │   └── CampaignInvariants.java        type rules and completeness over a snapshot
│   └── valueobject/
│       ├── CampaignId, StoreId, KioskId, AudienceRuleId, VetoId, MediaRef       identifiers
│       ├── CampaignName, MinimumOnScreenTime, PlaylistItem, Playlist
│       ├── Schedule, TimeSlot, Scope (sealed: AllKiosks | SelectedKiosks)
│       ├── CampaignType, CampaignStatus, Priority                               enums
│       └── CampaignSnapshot.java          immutable full state of a campaign
└── application/
    └── CampaignViews.java                 maps a Campaign to a CampaignView (R15)
```

Test sources (details in section 5):

```
src/test/java/com/cvretail/kiosko/campaigns/
├── bdd/                      Cucumber: suite runner, step definitions, shared world
├── domain/…                  unit tests by sub-package, CampaignBuilder, InMemoryCampaignRepository
└── application/              CampaignViewsTest
src/test/resources/
├── features/campaigns/*.feature      Gherkin scenarios, in English
└── junit-platform.properties         Cucumber reports and options
```

Dependency picture (every arrow follows `AGENTS.md`; the sub-package arrows are rule R19.3):

```
rules (later) ──► campaigns (CampaignView, CampaignCatalog)              published surface
campaigns.application ──► campaigns.domain, campaigns (surface)

inside campaigns.domain:
  exception ◄── valueobject ◄── event
                    ▲   ▲
                    │   └── service ──► exception
                    │
  entity ──► valueobject, event, exception, service
  repository ──► entity, valueobject
```

## 2. Decisions on the open questions

| Question | Decision | Why |
|---|---|---|
| Domain layout | Six sub-packages by building block: `entity`, `event`, `exception`, `repository`, `service`, `valueobject` (user decision, 2026-10-14) | Each kind of class has one obvious home; the dependency rules between them are tested (R19) |
| Where each class goes | `Campaign` in `entity`. `PlaylistItem` is a **value object**, not an entity: it has no identity of its own and is replaced as a whole. Enums and `CampaignSnapshot` in `valueobject`. `Violation` and its exception in `exception` | Keeps `entity` small and avoids cycles: the snapshot is the state that both the entity and the services read |
| Aggregate state | The **internal state is a `CampaignSnapshot`** (immutable record in `valueobject`); `snapshot()` simply returns it and mutators replace it | Resolves the entity ↔ service cycle: the services validate a snapshot and never see the entity |
| Domain services | Two stateless services: `CampaignStatusPolicy` (transition table) and `CampaignInvariants` (type rules and completeness) | They hold rules that are not the job of one value object and keep `Campaign` readable |
| Domain events | `Campaign` **records** the five status events and hands them over with `pullEvents()`; nothing publishes or listens (user decision) | Standard DDD; reports and the player can react later (KIO-54). Events carry no time because the domain has no clock |
| Aggregate style | **Mutable aggregate**; every mutator builds a candidate snapshot, validates it with `CampaignInvariants` and only then replaces the state and records its event | A rejected change leaves state and events untouched (R12.6, R20.4) |
| Domain errors | One unchecked `CampaignViolationException` carrying a **list** of `Violation(field, code, message)` | One type is enough for the API to build errors per field; a list reports every missing piece of an incomplete campaign at once |
| Bounds | Name 1–100 characters; playlist at most 50 items; item duration 1–3600 s; minimum on-screen time 1–3600 s, default 8 s | The proposals of the requirements, confirmed; named constants |
| Identifiers | Supplied by the caller: `Campaign.createRegular(CampaignId, StoreId, CampaignName)`; `CampaignId.generate()` is a convenience. `StoreId` and `KioskId` match `[A-Za-z0-9][A-Za-z0-9._-]{0,63}` | Tests are deterministic; the domain never draws random numbers by itself |
| `duplicate` | Not behavior of the aggregate: KIO-54 builds the copy with `snapshot()` and the public factories and mutators | A copy is a use case |
| `SCHEDULED` versus `ACTIVE` | `schedule()` requires a start date, and a `SCHEDULED` campaign cannot lose it. KIO-54 flips `SCHEDULED` → `ACTIVE` by date | The aggregate stays clock-free |
| Optional values | `Optional` components in records; `null` never crosses an API | A `DEFAULT` campaign has no priority: absence is part of the model |
| Published surface | The base package of `campaigns`; the view has its own nested enums, mapped with exhaustive `switch` expressions | The surface must not depend on `domain` and the domain must not depend on the surface (R15.2, R16.1) |
| BDD tool | **Cucumber-JVM 8.0.4** through the JUnit Platform engine, with PicoContainer for per-scenario state | Verified by a spike on 2026-10-14 (below) |
| Gherkin language | **English** (user decision) | Coherent with the code and the specs; business words are kept in the scenarios (R21.2) |
| BDD scope | **Hybrid** (user decision): scenarios for business behavior, unit tests for technical details | Avoids hundreds of technical scenarios and avoids duplication (R21.9) |
| Convention | Recorded in `AGENTS.md` for the whole backend (user decision) | Future domains (KIO-50, KIO-51, the rules) follow it |

### Cucumber spike (verified, not assumed)

A throwaway Maven project with the same parent (Spring Boot 4.1.1) was built outside the repository. Cucumber 8.0.4, with the Cucumber BOM imported, ran one scenario and one scenario outline (3 tests) under `mvn test` with **JUnit 6.0.3**, the version Spring Boot manages, although Cucumber 8.0.4 itself is built against JUnit 6.1.3. Resolved versions: `cucumber-java`, `cucumber-junit-platform-engine`, `cucumber-picocontainer`, `cucumber-core` and `cucumber-gherkin` 8.0.4; `cucumber-expressions` 20.1.0; `messages` 34.2.1; `junit-platform-suite` 6.0.3. Findings:

- The suite is selected with `@SelectPackages("features")` (the resource selector gives a warning).
- Spanish and English Gherkin both work; English is used here.
- A `*Test` suite class is picked up by Surefire with no extra configuration.
- Licenses: Cucumber-JVM is MIT (upstream `LICENSE`); PicoContainer is BSD (its parent POM). Both are test scope.
- The coupling between the Cucumber and the JUnit versions must be rechecked whenever Spring Boot changes the JUnit version.

## 3. Domain model

### 3.1 Value objects (`valueobject`)

All are immutable `record`s (or sealed interfaces of records) whose compact constructor validates and throws `CampaignViolationException`.

| Type | Content and rule | Violation code |
|---|---|---|
| `CampaignId` | wraps a `UUID`; `generate()` | `id.required` |
| `StoreId`, `KioskId` | text matching `[A-Za-z0-9][A-Za-z0-9._-]{0,63}` | `store.invalid`, `kiosk.invalid` |
| `AudienceRuleId`, `VetoId` | wrap a `UUID` | `audience_rule.invalid`, `veto.invalid` |
| `MediaRef` | `sha256:` plus 64 lowercase hex characters | `media_ref.invalid` |
| `CampaignName` | trimmed text, 1–100 characters | `name.blank`, `name.too_long` |
| `MinimumOnScreenTime` | whole seconds 1–3600; `DEFAULT_SECONDS = 8` | `minimum_on_screen.out_of_range` |
| `PlaylistItem` | `MediaRef` plus duration in seconds 1–3600 | `playlist.item.duration_out_of_range` |
| `Playlist` | ordered list of at most 50 items | `playlist.too_many_items`, `playlist.position_out_of_range` |
| `TimeSlot` | start and end `LocalTime`, different; start later than end crosses midnight | `schedule.slot.empty_range` |
| `Schedule` | `Optional<LocalDate>` start and end, `Set<DayOfWeek>` days, `Optional<TimeSlot>`; `Schedule.always()` | `schedule.end_before_start`, `schedule.days.empty` |
| `Scope` | sealed: `AllKiosks` or `SelectedKiosks(Set<KioskId>)`, non-empty, built by `SelectedKiosks.of(Collection)` which rejects duplicates | `scope.kiosks.empty`, `scope.kiosks.duplicate` |
| `CampaignType`, `CampaignStatus`, `Priority` | enums: `REGULAR`/`DEFAULT`; `DRAFT`/`SCHEDULED`/`ACTIVE`/`PAUSED`/`FINISHED`; `HIGH`/`MEDIUM`/`LOW` | — |
| `CampaignSnapshot` | id, store, type, name, status, `Optional<Priority>`, `Optional<AudienceRuleId>`, `Playlist`, `Schedule`, `Scope`, `Optional<MinimumOnScreenTime>`, `Set<VetoId>`; `with…` methods return a modified copy | — |

A required value that is `null` is reported as `<field>.required`. `Playlist` operations return a new `Playlist`: `add`, `insert(position, item)`, `remove(position)`, `move(from, to)`, `withDuration(position, seconds)`; positions are zero-based and the list never has gaps.

### 3.2 The aggregate (`entity`)

```java
public final class Campaign {
    // creation: always DRAFT, incomplete allowed; records CampaignCreated
    public static Campaign createRegular(CampaignId id, StoreId storeId, CampaignName name);
    public static Campaign createDefault(CampaignId id, StoreId storeId, CampaignName name);
    public static Campaign restore(CampaignSnapshot snapshot);       // validates everything; no events

    // reads
    CampaignId id(); StoreId storeId(); CampaignType type(); CampaignName name(); CampaignStatus status();
    Optional<Priority> priority(); Optional<AudienceRuleId> audienceRuleId(); Playlist playlist();
    Schedule runSchedule(); Scope scope(); Optional<MinimumOnScreenTime> minimumOnScreenTime();
    Set<VetoId> manualVetoes(); CampaignSnapshot snapshot();
    List<CampaignEvent> pullEvents();                                  // hands over and forgets

    // editing (any status except FINISHED)
    void rename(CampaignName name);
    void changePriority(Priority priority);                           // REGULAR only
    void assignAudienceRule(AudienceRuleId id);  void clearAudienceRule();   // REGULAR only
    void addPlaylistItem(PlaylistItem item);  void insertPlaylistItem(int position, PlaylistItem item);
    void removePlaylistItem(int position);  void movePlaylistItem(int from, int to);
    void changePlaylistItemDuration(int position, int seconds);
    void reschedule(Schedule schedule);                               // REGULAR only
    void changeScope(Scope scope);
    void changeMinimumOnScreenTime(MinimumOnScreenTime time);         // REGULAR only
    void addManualVeto(VetoId id);  void removeManualVeto(VetoId id);     // REGULAR only

    // transitions (each records its event)
    void schedule();  void activate();  void pause();  void finish();
}
```

`equals` and `hashCode` use the `CampaignId` only (R1.6). Every edit and every transition goes through the same steps:

```java
private void change(UnaryOperator<CampaignSnapshot> edit, Supplier<CampaignEvent> eventOrNull) {
    CampaignInvariants.requireNotFinished(state);        // status.finished
    CampaignSnapshot candidate = edit.apply(state);      // value objects validate when built
    CampaignInvariants.validate(candidate);              // type rules + completeness if SCHEDULED or ACTIVE
    state = candidate;                                   // commit only after every check
    record(eventOrNull);                                 // after the commit, so a rejection records nothing
}
```

Transitions call `CampaignStatusPolicy.requireAllowed(state.status(), transition)` first, then `change` with `withStatus(newStatus)`.

### 3.3 Domain services (`service`)

- **`CampaignStatusPolicy`** (stateless): the table of 3.5 as data; `requireAllowed(status, transition)` throws `status.transition_not_allowed`, naming the current status and the transition, or `status.finished` for `FINISHED`.
- **`CampaignInvariants`** (stateless), over a `CampaignSnapshot`:
  - `requireNotFinished`;
  - type rules: priority, audience rule, minimum on-screen time, manual vetoes and schedule per type (table below);
  - completeness, required only when the candidate status is `SCHEDULED` or `ACTIVE`.

| Field | `REGULAR` | `DEFAULT` |
|---|---|---|
| priority | present, default `MEDIUM` | absent (`priority.not_allowed_for_default`) |
| audience rule | optional | absent (`audience_rule.not_allowed_for_default`) |
| minimum on-screen time | present, default 8 s | absent (`minimum_on_screen.not_allowed_for_default`) |
| manual vetoes | any | empty (`vetoes.not_allowed_for_default`) |
| schedule | any | `Schedule.always()`, not changeable (`schedule.not_changeable_for_default`) |

### 3.4 Completeness

`CampaignInvariants` reports every missing piece in one exception:

| Missing | Code | Applies to |
|---|---|---|
| playlist has no item | `playlist.empty` | both types |
| no audience rule | `audience_rule.required` | `REGULAR` |
| no start date while `SCHEDULED` | `schedule.start_date_required` | `REGULAR`, status `SCHEDULED` |

The name and the schedule are valid by construction. `DRAFT` and `PAUSED` accept incomplete states; `FINISHED` accepts no change.

### 3.5 Status transitions

| Transition | Allowed from | Result | Extra requirement |
|---|---|---|---|
| `schedule()` | `DRAFT` | `SCHEDULED` | complete, with a start date |
| `activate()` | `DRAFT`, `SCHEDULED`, `PAUSED` | `ACTIVE` | complete |
| `pause()` | `SCHEDULED`, `ACTIVE` | `PAUSED` | none |
| `finish()` | `SCHEDULED`, `ACTIVE`, `PAUSED` | `FINISHED` | none |

```
DRAFT ──schedule──► SCHEDULED ──activate──► ACTIVE ──pause──► PAUSED ──activate──► ACTIVE
  │                    │  └──────pause───────────────────────► PAUSED
  └────────activate────┴────────────────────────────────────► ACTIVE
SCHEDULED / ACTIVE / PAUSED ──finish──► FINISHED (terminal)
```

### 3.6 Domain events (`event`)

```java
public sealed interface CampaignEvent
        permits CampaignCreated, CampaignScheduled, CampaignActivated, CampaignPaused, CampaignFinished {
    CampaignId campaignId();
    StoreId storeId();
}
public record CampaignCreated(CampaignId campaignId, StoreId storeId, CampaignType type) implements CampaignEvent {}
public record CampaignScheduled(CampaignId campaignId, StoreId storeId) implements CampaignEvent {}
// CampaignActivated, CampaignPaused, CampaignFinished: same shape
```

`event` depends only on `valueobject`. The aggregate keeps a private list; `pullEvents()` returns an unmodifiable copy and clears it. Edits that are not status transitions record nothing in this feature.

### 3.7 Domain errors (`exception`)

```java
public record Violation(String field, String code, String message) {}
public final class CampaignViolationException extends RuntimeException {
    public List<Violation> violations();          // never empty
}
```

Messages are English; codes and fields are stable identifiers. Catalog: `<field>.required`; `id.required`, `store.invalid`, `kiosk.invalid`, `audience_rule.invalid`, `veto.invalid`, `media_ref.invalid`; `name.blank`, `name.too_long`; the five `*.not_allowed_for_default` and `schedule.not_changeable_for_default` of 3.3; `minimum_on_screen.out_of_range`, `playlist.item.duration_out_of_range`, `playlist.too_many_items`, `playlist.position_out_of_range`; `schedule.end_before_start`, `schedule.days.empty`, `schedule.slot.empty_range`, `scope.kiosks.empty`, `scope.kiosks.duplicate`; `playlist.empty`, `audience_rule.required`, `schedule.start_date_required`; `status.transition_not_allowed`, `status.finished`. The codes live as constants in one holder so a test can assert that each is raised by a scenario.

### 3.8 Repository port (`repository`)

```java
public interface CampaignRepository {
    void save(Campaign campaign);                              // insert or update
    Optional<Campaign> findById(StoreId storeId, CampaignId id);
    List<Campaign> findAllByStore(StoreId storeId);
    Optional<Campaign> findDefault(StoreId storeId);
    boolean deleteById(StoreId storeId, CampaignId id);        // true if it existed
}
```

Every operation takes the store (R14.3). "Exactly one `DEFAULT` per store" crosses aggregates: KIO-56 enforces it in a use case and KIO-49 adds a unique constraint. The in-memory implementation lives in the test sources.

## 4. Published surface (R15)

In `com.cvretail.kiosko.campaigns` (unchanged by the reorganization):

```java
public record CampaignView(
        UUID id, String storeId, Type type, Status status, Optional<Priority> priority,
        Schedule schedule, Scope scope, Optional<Integer> minimumOnScreenSeconds,
        Optional<UUID> audienceRuleId, Set<UUID> manualVetoIds, List<PlaylistItem> playlist) {
    public enum Type { REGULAR, DEFAULT }
    public enum Status { DRAFT, SCHEDULED, ACTIVE, PAUSED, FINISHED }
    public enum Priority { HIGH, MEDIUM, LOW }
    public record PlaylistItem(String mediaRef, int durationSeconds) {}
    public record Schedule(Optional<LocalDate> startDate, Optional<LocalDate> endDate,
                           Set<DayOfWeek> days, Optional<TimeSlot> slot) {}
    public record TimeSlot(LocalTime start, LocalTime end) {}
    public record Scope(boolean allKiosks, Set<String> kioskIds) {}
}
public interface CampaignCatalog {
    List<CampaignView> findByStatus(String storeId, CampaignView.Status status);
    Optional<CampaignView> findDefault(String storeId);
}
```

`campaigns.application.CampaignViews.from(Campaign)` builds the view from `snapshot()`, with exhaustive `switch` expressions for the enums. The adapter that implements `CampaignCatalog` is KIO-49 and later (R15.4).

## 5. Tests

### 5.1 Cucumber (BDD, R21)

**Dependencies** (all test scope, R18.3). In `pom.xml`: a property `cucumber.version` = `8.0.4`, the `cucumber-bom` imported in `dependencyManagement`, and the test dependencies `cucumber-java`, `cucumber-junit-platform-engine`, `cucumber-picocontainer` and `junit-platform-suite` (its version is already managed by Spring Boot).

**Layout**

```
src/test/resources/features/campaigns/
├── campaign_creation.feature         @R1
├── campaign_lifecycle.feature        @R11   (scenario outline: the status × transition matrix)
├── campaign_completeness.feature     @R12
├── campaign_editing.feature          @R3 @R4 @R8 @R9 @R12
├── default_campaign.feature          @R10
├── playlist.feature                  @R5
├── schedule_and_scope.feature        @R6 @R7
└── domain_events.feature             @R20
src/test/java/com/cvretail/kiosko/campaigns/bdd/
├── CampaignDomainBehaviourTest.java  suite runner (named *Test so Surefire runs it)
├── CampaignWorld.java                per-scenario state (injected by PicoContainer)
├── RejectionReasons.java             business phrase → violation code
└── *Steps.java                       one class per feature area
```

**Runner**

```java
@Suite
@IncludeEngines("cucumber")
@SelectPackages("features.campaigns")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "com.cvretail.kiosko.campaigns.bdd")
class CampaignDomainBehaviourTest {}
```

`junit-platform.properties` sets the report plugin (`html:target/cucumber-reports/campaigns.html`) and quiets the publish banner; reports live under `target/` (R21.8).

**Shared state**: `CampaignWorld` (current campaign, store, the last rejection, the pulled events) is created per scenario and injected into each steps class by constructor (R21.3); there are no static fields. Steps build campaigns with `CampaignBuilder`.

**Business language** (R21.2). A scenario reads like this; the step maps the phrase to a stable code through `RejectionReasons`:

```gherkin
@R12
Feature: A running campaign must stay complete

  Background:
    Given a store "tienda-001"

  @R12.3
  Scenario: An active campaign cannot lose its last playlist item
    Given an active regular campaign with one playlist item
    When the administrator removes the playlist item
    Then the change is rejected because the playlist is empty
    And the campaign keeps its playlist item

  @R11.2
  Scenario Outline: Status transitions
    Given a complete regular campaign in status "<from>"
    When the administrator tries to <transition> the campaign
    Then the outcome is "<outcome>"

    Examples:
      | from      | transition | outcome                            |
      | draft     | schedule   | the campaign becomes scheduled     |
      | draft     | pause      | rejected: transition not allowed   |
      | finished  | activate   | rejected: the campaign is finished |
```

**Running** (documented in the README, R17.6): all scenarios and unit tests with `./mvnw test`; only the scenarios with `./mvnw -Dtest=CampaignDomainBehaviourTest test`; one tag with `-Dcucumber.filter.tags="@R11"`; one feature with `-Dcucumber.features=src/test/resources/features/campaigns/campaign_lifecycle.feature`. The two filters are confirmed in the implementation tasks because Surefire must forward the properties.

### 5.2 Unit tests (technical details)

All are named `*Test`, use no Spring, database or network, and run with `./mvnw test`.

| Test class | Covers |
|---|---|
| `valueobject/IdentifiersTest` | `CampaignId`, `StoreId`, `KioskId`, `MediaRef` rules and equality (R1.2, R1.6, R5.2) |
| `valueobject/CampaignNameTest` | trimming, blank, maximum length (R2) |
| `valueobject/PlaylistTest`, `MinimumOnScreenTimeTest` | bounds and operations of the value objects (R5.3, R5.5, R8.1) |
| `valueobject/ScheduleTest`, `ScopeTest` | construction rules at value-object level (R6, R7) |
| `valueobject/CampaignSnapshotTest` | round trip and `restore` rejecting every invalid persisted state (R12.5) |
| `service/CampaignStatusPolicyTest`, `CampaignInvariantsTest` | the policy table and each type rule and completeness code at service level (R11, R12) |
| `exception/ViolationCodesTest` | every code of the catalog is raised by at least one test or scenario (R13) |
| `repository/CampaignRepositoryContractTest` (abstract) and `InMemoryCampaignRepositoryTest` | the port contract: save and find, store isolation, default lookup, delete result (R14); reusable by KIO-49 |
| `application/CampaignViewsTest` | mapping of every field for `REGULAR` and `DEFAULT`; enum name parity (R15.1, R15.2) |

Test helpers: `CampaignBuilder` (for example `aRegularCampaign().withPlaylistItems(2).withAudienceRule().scheduled()`) used by both the scenarios and the unit tests (R16.4), and `InMemoryCampaignRepository`.

### 5.3 Traceability: where each requirement is verified (R21.6)

| Requirement | Scenarios (feature) | Unit tests |
|---|---|---|
| R1 creation and identity | `campaign_creation` | `IdentifiersTest` |
| R2 name | — | `CampaignNameTest` |
| R3 priority, R4 audience rule | `campaign_editing`, `default_campaign` | `CampaignInvariantsTest` |
| R5 playlist | `playlist` | `PlaylistTest` |
| R6 schedule, R7 scope | `schedule_and_scope` | `ScheduleTest`, `ScopeTest` |
| R8 minimum time, R9 vetoes | `campaign_editing`, `default_campaign` | `MinimumOnScreenTimeTest` |
| R10 default campaign | `default_campaign` | — |
| R11 status and transitions | `campaign_lifecycle` | `CampaignStatusPolicyTest` |
| R12 completeness and editing | `campaign_completeness`, `campaign_editing` | `CampaignInvariantsTest`, `CampaignSnapshotTest` |
| R13 domain errors | assertions in every rejection scenario | `ViolationCodesTest` |
| R14 repository port | — | contract and in-memory tests |
| R15 published view | — | `CampaignViewsTest`, `ModularityTest` |
| R19 structure | — | `ArchitectureTest` |
| R20 events | `domain_events` | — |

### 5.4 Architecture and module tests

- `ArchitectureRules.PUBLISHED_SURFACE_IS_SELF_CONTAINED`: classes directly in a module's base package (`com.cvretail.kiosko.*`, excluding `package-info`) shall not depend on `..domain..`, `..application..`, `..infrastructure..` or frameworks.
- `ArchitectureRules.DOMAIN_INTERNAL_LAYERING` (R19.3 and R19.5), generic for every module:

```java
layeredArchitecture().consideringOnlyDependenciesInLayers()
    .layer("Exception").definedBy("..domain.exception..")
    .layer("ValueObject").definedBy("..domain.valueobject..")
    .layer("Event").definedBy("..domain.event..")
    .layer("Service").definedBy("..domain.service..")
    .layer("Entity").definedBy("..domain.entity..")
    .layer("Repository").definedBy("..domain.repository..")
    .whereLayer("Repository").mayNotBeAccessedByAnyLayer()
    .whereLayer("Entity").mayOnlyBeAccessedByLayers("Repository")
    .whereLayer("Service").mayOnlyBeAccessedByLayers("Entity")
    .whereLayer("Event").mayOnlyBeAccessedByLayers("Entity")
    .whereLayer("ValueObject").mayOnlyBeAccessedByLayers("Event", "Service", "Entity", "Repository")
    .whereLayer("Exception").mayOnlyBeAccessedByLayers("ValueObject", "Service", "Entity")
    .allowEmptyShould(true);
```

- `ArchitectureTest` runs both rules and the existing ones on the production classes; `ArchitectureRulesDetectViolationsTest` gets fixtures that break each new rule (a value object using an entity, a view using a domain type) and asserts that the build fails naming the class.
- `ModularityTest` adds two assertions with `ApplicationModule.isExposed`: `CampaignView` and `CampaignCatalog` are exposed by `campaigns`, `Campaign` is not (R15.5). `verify()` already fails if `rules` uses `campaigns` internals.

## 6. Documentation (R17)

- `services/core-backend/README.md`: a "Campaign domain" section (structure by building block, the status diagram, the type rules and completeness tables, the published surface, the error codes), the glossary of Spanish business terms to English identifiers, and a "Behavior-driven specification" section (where the features live, how to run all, one feature and one tag, how tags trace to requirements).
- `AGENTS.md`: the status line; the note that domain code started with KIO-24; the sub-package structure of a domain; the BDD convention (R17.5) and the commands to run scenarios.
- The Trello card KIO-24 is updated at the end.

## 7. Verification strategy

- `./mvnw test` runs scenarios and unit tests together and passes in a few seconds; `./mvnw -B -ntp verify` passes.
- The two scenario filters work with Surefire (`-Dcucumber.filter.tags`, `-Dcucumber.features`), or the README states the working form.
- **Negative checks**, then undone:
  - remove one business rule (for example the transition guard) and confirm that a scenario fails;
  - remove one value-object bound and confirm that a unit test fails;
  - add a Spring import to a class in `campaigns/domain` and confirm that `ArchitectureTest` fails;
  - make a value object use an entity and confirm that the layering rule fails;
  - make `CampaignView` reference a domain type and confirm that the surface rule fails;
  - make a class in `rules` use `campaigns.domain.entity.Campaign` and confirm that `ModularityTest` fails.
- `git diff --stat` shows no change in `contracts/`, no migration and no runtime dependency; the `pom.xml` diff adds only the Cucumber BOM, the four test dependencies and one property.
- Licenses of the new test dependencies are recorded in this file (below) and re-read from the resolved tree.
- The `Core backend` CI job stays green without a workflow change.

Licenses (R18.3): `cucumber-java`, `cucumber-junit-platform-engine`, `cucumber-picocontainer` and the rest of Cucumber-JVM: MIT (upstream `LICENSE`); PicoContainer: BSD (its parent POM); `junit-platform-suite`: EPL-2.0 like the rest of JUnit, already part of Spring Boot's test stack. Confirmed from upstream sources on 2026-10-14; the JUnit item is from knowledge and is rechecked in the tasks.

## 8. Requirement coverage

| Requirement | Covered by |
|---|---|
| R1–R10 | 3.1, 3.2, 3.3; scenarios and unit tests as in 5.3 |
| R11 | 3.3, 3.5; `campaign_lifecycle`, `CampaignStatusPolicyTest` |
| R12 | 3.2 (`change`), 3.3, 3.4; `campaign_completeness`, `campaign_editing` |
| R13 | 3.7, `ViolationCodesTest` |
| R14 | 3.8, contract tests |
| R15 | 4, `CampaignViewsTest`, `ModularityTest` |
| R16 | 5, 7 |
| R17 | 6 |
| R18 | 1, 3.1, 7 |
| R19 Domain structure | 1, 3.1–3.8, 5.4 |
| R20 Domain events | 3.2, 3.6; `domain_events` |
| R21 BDD with Cucumber | 2 (spike), 5.1, 5.3, 6, 7 |

## 9. Differences from the requirements

- The aggregate's internal state is the `CampaignSnapshot` value object (open question 1 of the requirements), instead of a private record; the effect is the same and avoids a package cycle.
- `PlaylistItem` is a value object, not an entity.
- The layering rule is exactly R19.3 (`Exception` is used only by `ValueObject`, `Service` and `Entity`).
- **R15.5** is checked with `ApplicationModule.isExposed` in `ModularityTest`.
- **R14.6** is met with the in-memory repository in the test sources, plus a reusable abstract contract test.
- The reader of the schedule is `runSchedule()` because `schedule()` is the status transition.
- `audience_rule.invalid` and `veto.invalid` do not exist: those identifiers wrap a `UUID` and can only be missing (`audienceRuleId.required`, `vetoId.required`). Added `veto.duplicate` and `veto.not_found` (R9.2), `priority.required` and `minimum_on_screen.required` (a `REGULAR` campaign must keep both).
- `SCHEDULED` requires a start date while it stays `SCHEDULED`, which makes explicit what R11.2 states for the transition only.

## 10. Risks and open points

- **Mutable aggregate protected by discipline:** every mutator must go through `change(...)`. Tests check that a rejected change leaves state and events untouched for every mutator, and a review check ("no assignment to the state outside `change`") is part of the tasks.
- **Cucumber and JUnit version coupling:** Cucumber 8.0.4 is built against JUnit 6.1.3 and was verified against 6.0.3; revisit when Spring Boot moves JUnit.
- **Scenario sprawl and step duplication:** mitigated by the hybrid scope, one steps class per feature area, and the `RejectionReasons` and `CampaignBuilder` helpers.
- **Duplicated enums in the view:** the cost of an independent published surface; exhaustive `switch` and the parity test protect it.
- **`SCHEDULED` meaning:** the business documents do not distinguish it from `ACTIVE`; only 3.3, 3.4 and 3.5 change if the team decides otherwise.
- **One `DEFAULT` per store** is not enforced by the aggregate or the port: KIO-56 and KIO-49 must enforce it.
- **Contract priority:** the contract keeps a numeric priority; the mapping and any schema change are KIO-30's and need approval.
- **Size:** about 33 production classes and 16 test classes plus 8 feature files. Tasks are ordered so each step compiles and passes before the next.
