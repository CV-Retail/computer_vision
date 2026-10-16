# KIO-25 Rules: schedule and scope: design

Implements `requirements.md` (R1–R13). Scope: step 1 of the rules engine in the `rules` module of `core-backend`: the evaluation of "does this campaign's calendar and scope allow it to run on this kiosk now", the use case that applies it to the campaigns of a store, and the Cucumber scenarios, unit tests and architecture rules that prove it. No persistence, no Spring wiring, no configuration, no runtime dependency, no migration, no change to `contracts/` or to the published surface of `campaigns`.

## 1. Overview

```
services/core-backend/src/main/java/com/cvretail/kiosko/rules/
├── package-info.java                      (exists) published surface: empty on purpose (R9.5)
├── application/
│   ├── FindCandidateCampaigns.java        use case: candidates for a kiosk now; availability of one campaign
│   └── CampaignRunPlans.java              maps a CampaignView to the domain's CampaignRunPlan (package-private)
└── domain/
    ├── service/
    │   └── AvailabilityEvaluator.java     the rules of R2–R6, a pure function
    └── valueobject/
        ├── CampaignRunPlan.java           what the evaluator needs from a campaign
        ├── CampaignLifecycle.java         enum DRAFT, SCHEDULED, ACTIVE, PAUSED, FINISHED
        ├── ScheduleWindow.java            dates, days, optional slot
        ├── DailySlot.java                 start and end time of day
        ├── KioskSelection.java            sealed: AllKiosks | SelectedKiosks
        ├── KioskId.java                   non-blank text
        └── Availability.java              enum: APPLIES or the reason it does not
```

Test sources:

```
src/test/java/com/cvretail/kiosko/rules/
├── bdd/                      Cucumber: suite runner, steps, world
├── support/                  CampaignViewBuilder, InMemoryCampaignCatalog (fake of the published port)
├── domain/…                  unit tests by sub-package
└── application/              FindCandidateCampaignsTest, CampaignRunPlansTest
src/test/resources/features/rules/*.feature
```

Dependency picture (R9):

```
rules.application ──► rules.domain
rules.application ──► campaigns (CampaignView, CampaignCatalog)       published surface only
rules.domain      ──► JDK only                                        never campaigns

inside rules.domain:  service ──► valueobject          (sub-package rules of AGENTS.md apply)
```

The published surface of `rules` stays empty (R9.5): nobody outside the module needs the use case until KIO-28 composes the engine.

## 2. Decisions on the open questions

| Question | Decision | Why |
|---|---|---|
| 1 Statuses | `ACTIVE` **and** `SCHEDULED` are evaluated; `DRAFT`, `PAUSED`, `FINISHED` never (R2) | The engine does not depend on when the KIO-54 job flips `SCHEDULED` to `ACTIVE`; a `SCHEDULED` campaign before its start date is excluded anyway by the start date |
| 2 Slot that crosses midnight | The part after midnight belongs to the **schedule day** on which the slot started; days of the week, start date and end date are checked against that day (R5.4) | It matches what "Monday 22:00 to 02:00" means to an administrator. Section 3.2 gives the algorithm and a reference table |
| 3 End of a slot | End time **excluded** (R5.3); start time included | Two consecutive slots 08:00–12:00 and 12:00–20:00 never overlap or leave a gap |
| 4 Campaign type | The evaluator ignores the type (R2.4). The view's type is not even mapped | KIO-28 chooses between `REGULAR` campaigns and the `DEFAULT` one |
| 5 Zone until KIO-62 | Only the zone of the injected `Clock` (R7). No bean, property or variable here | KIO-62 provides the configured `Clock`; until then the use case is built by hand in tests |
| 6 Reasons | `Availability` carries the reason. The use case returns only the candidates (`List<CampaignView>`); the reason is available through `availabilityOf(view, kioskId)` for tests and later views (R8.3) | Cheap, deterministic and testable, without widening what is sent to the player |
| 7 Second Cucumber suite | Runner `RulesBehaviourTest`, glue `com.cvretail.kiosko.rules.bdd`, features in `features.rules`; the report plugin moves from the shared `junit-platform.properties` to each runner (section 5.1) | Two suites would otherwise write the same report file (R11.6) |
| 8 Data in `CampaignView` | Enough: status, schedule (dates, days, slot), scope and store id are all there. **No change to `campaigns`** | R9.4 |
| 9 Where the use case lives | `rules.application`, package-visible to the module, not in the base package | R9.5 |
| Evaluation input | The evaluator takes a `LocalDateTime`, the local date and time of the moment; the use case does the conversion from the clock | The domain stays free of clocks and zones: it is a function of local wall time. Zone and DST behavior are properties of the conversion and are tested at the use-case level |
| Candidate order | The catalog is asked twice, `ACTIVE` first and then `SCHEDULED`, and the candidates keep that order (each call keeps the catalog's order) (R1.4) | `CampaignCatalog` has no query for several statuses and the surface is not changed (R9.4) |
| Input checks | `KioskId` and the store id reject null or blank with `IllegalArgumentException`; the value objects of the domain reject inconsistent data with `IllegalArgumentException` | These are programming or mapping errors, not business violations, so `CampaignViolationException` (a campaigns type) is not reused |
| Tags | Every rules feature carries the task tag `@KIO-25` besides the requirement ids; the eight campaigns features get `@KIO-24` | `@R5` means "playlist" in KIO-24 and "time slot" in KIO-25; `@KIO-25 and @R5` is unambiguous (section 5.1) |

## 3. Domain

### 3.1 Value objects (`valueobject`)

Immutable records. Inconsistent data throws `IllegalArgumentException`.

| Type | Content and rule |
|---|---|
| `CampaignLifecycle` | `DRAFT`, `SCHEDULED`, `ACTIVE`, `PAUSED`, `FINISHED`; `isEligible()` is true for `SCHEDULED` and `ACTIVE` |
| `DailySlot` | `LocalTime start`, `LocalTime end`, different; `crossesMidnight()` is `start.isAfter(end)` |
| `ScheduleWindow` | `Optional<LocalDate> startDate`, `Optional<LocalDate> endDate` (not before the start), `Set<DayOfWeek> days` (non-empty), `Optional<DailySlot> slot` |
| `KioskId` | non-blank text, compared exactly |
| `KioskSelection` | sealed: `AllKiosks`, or `SelectedKiosks(Set<KioskId>)` non-empty; `includes(KioskId)` |
| `CampaignRunPlan` | `UUID id`, `CampaignLifecycle lifecycle`, `ScheduleWindow window`, `KioskSelection kiosks` |
| `Availability` | `APPLIES`, `STATUS_NOT_ELIGIBLE`, `BEFORE_START_DATE`, `AFTER_END_DATE`, `DAY_NOT_INCLUDED`, `OUTSIDE_TIME_SLOT`, `KIOSK_NOT_INCLUDED` (the order of R8.1); `applies()` |

### 3.2 The evaluator (`service`)

```java
public final class AvailabilityEvaluator {
    public static Availability evaluate(CampaignRunPlan plan, KioskId kiosk, LocalDateTime localNow);
}
```

Algorithm (stateless, no clock):

```
1. if !plan.lifecycle.isEligible()                      -> STATUS_NOT_ELIGIBLE
2. date = localNow.toLocalDate(); time = localNow.toLocalTime(); slot = plan.window.slot
3. scheduleDay = date
   if slot present and slot.crossesMidnight and time < slot.end:  scheduleDay = date - 1 day
4. if startDate present and scheduleDay < startDate     -> BEFORE_START_DATE
5. if endDate   present and scheduleDay > endDate       -> AFTER_END_DATE
6. if scheduleDay.dayOfWeek not in days                 -> DAY_NOT_INCLUDED
7. if slot present and !inSlot(slot, time)              -> OUTSIDE_TIME_SLOT
8. if !plan.kiosks.includes(kiosk)                      -> KIOSK_NOT_INCLUDED
9. APPLIES

inSlot(slot, t):  non-crossing:  start <= t < end
                  crossing:      t >= start  or  t < end
```

The checks run in the order of R8.1, so the first reason wins (R8.2). Step 3 is the schedule-day rule (R5.4). For a crossing slot, a time in `[end, start)` (the gap of the day) keeps `scheduleDay = date`.

Reference table (the same one becomes the scenario outline). Campaign: Monday only (`2030-01-14` is a Monday), start and end date `2030-01-14`, slot `22:00` to `02:00`:

| Local moment | Schedule day | Result |
|---|---|---|
| Mon 2030-01-14 21:59 | Mon 14 | `OUTSIDE_TIME_SLOT` |
| Mon 2030-01-14 22:00 | Mon 14 | `APPLIES` |
| Mon 2030-01-14 23:59:59 | Mon 14 | `APPLIES` |
| Tue 2030-01-15 00:00 | Mon 14 | `APPLIES` |
| Tue 2030-01-15 01:59:59 | Mon 14 | `APPLIES` |
| Tue 2030-01-15 02:00 | Tue 15 | `AFTER_END_DATE` |
| Mon 2030-01-14 01:00 | Sun 13 | `BEFORE_START_DATE` |

### 3.3 Time zone and daylight saving time

The use case converts with `ZonedDateTime.now(clock).toLocalDateTime()`. This is a total function: every instant has one local date and time, including the repeated hour (the clocks move back) and the skipped hour (they move forward), so it never fails (R7.4). The consequence, accepted and tested, is that rules are about local wall time:

| Day (Europe/Madrid, 2030) | Effect on a slot |
|---|---|
| 31 March, clock 02:00 → 03:00 | Slot `02:00–04:00` applies from 03:00 local (instant 01:00Z) to 04:00; the slot `02:30–02:45` never applies that day |
| 27 October, clock 03:00 → 02:00 | Slot `02:00–03:00` applies during two real hours, because 02:30 happens twice; both instants give the same answer |

## 4. Application

```java
public final class FindCandidateCampaigns {
    public FindCandidateCampaigns(CampaignCatalog catalog, Clock clock);
    public List<CampaignView> candidatesFor(String storeId, String kioskId);
    public Availability availabilityOf(CampaignView campaign, String kioskId);
}
```

`candidatesFor`:

1. Rejects a blank `storeId` or `kioskId` (`IllegalArgumentException`).
2. Reads `now = ZonedDateTime.now(clock).toLocalDateTime()` **once**, so every campaign is judged at the same moment.
3. Asks the catalog for the store's `ACTIVE` campaigns and then its `SCHEDULED` ones (R2.3, R1.4).
4. Keeps the views whose `storeId` equals the requested one (defensive, R1.5), maps each with `CampaignRunPlans.from(view)` and keeps those for which the evaluator answers `APPLIES`.
5. Returns an unmodifiable list; an empty list when nothing applies (R1.6). It writes nothing (R1.7).

`availabilityOf` runs steps 2 and 4 for one view; it is the code path of R8.3.

`CampaignRunPlans.from` maps: id; status with an exhaustive `switch` over `CampaignView.Status`; the schedule (`startDate`, `endDate`, `days`, `slot`) and the scope (`allKiosks` → `AllKiosks`, otherwise `SelectedKiosks` of the ids). It is the only class of the module that knows `CampaignView`.

## 5. Tests

### 5.1 Cucumber (BDD, R11)

**Layout**

```
src/test/resources/features/rules/
├── campaign_status.feature            @KIO-25 @R2
├── schedule_dates.feature             @KIO-25 @R3
├── schedule_days.feature              @KIO-25 @R4
├── schedule_time_slot.feature         @KIO-25 @R5   (outline: moments around a slot; outline: crossing midnight, table of 3.2)
├── kiosk_scope.feature                @KIO-25 @R6
└── candidate_campaigns.feature        @KIO-25 @R1 @R7 @R8   (zones, order, store, empty result)
src/test/java/com/cvretail/kiosko/rules/bdd/
├── RulesBehaviourTest.java            suite runner (a *Test class)
├── RulesWorld.java                    scenario state: clock, store, catalog, last result
└── StatusSteps, ScheduleSteps, ScopeSteps, CandidateSteps
```

**Runner**

```java
@Suite
@IncludeEngines("cucumber")
@SelectPackages("features.rules")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "com.cvretail.kiosko.rules.bdd")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "html:target/cucumber-reports/rules.html")
class RulesBehaviourTest {}
```

The report plugin line is **removed from `junit-platform.properties`** and added as a `PLUGIN_PROPERTY_NAME` parameter to `CampaignDomainBehaviourTest` (`campaigns.html`) and to the new runner (`rules.html`), so the files no longer overwrite each other. `cucumber.publish.quiet=true` stays in the shared file.

**Business language** (R11.2). Examples of steps:

```gherkin
@KIO-25 @R5
Feature: A campaign runs only inside its time slot

  Background:
    Given a store "tienda-001" and a kiosk "kiosk-a"
    And the server time zone is "Europe/Madrid"

  @R5.2 @R5.3
  Scenario Outline: Moments around a slot from 09:00 to 18:00
    Given an active campaign running every day from 09:00 to 18:00
    When the server time is "<moment>"
    Then the campaign is <outcome> for the kiosk

    Examples:
      | moment           | outcome       |
      | 2030-01-14 08:59 | not a candidate |
      | 2030-01-14 09:00 | a candidate   |
      | 2030-01-14 17:59 | a candidate   |
      | 2030-01-14 18:00 | not a candidate |
```

The world holds a `Clock.fixed` built from the step "the server time is" and the zone step; the steps build `CampaignView`s with the test builder and a fake catalog; no static state (PicoContainer per scenario, as in KIO-24). A step "the reason is …" maps business phrases (`the end date has passed`) to `Availability` values.

**Tags and running.** Because `@R5` also exists in KIO-24, a tag selection names the task as well. README and `AGENTS.md` document:

```bash
./mvnw -Dtest=RulesBehaviourTest test                                                     # only the rules scenarios
./mvnw -Dtest=RulesBehaviourTest -Dcucumber.filter.tags="@KIO-25 and @R5" test             # one requirement
./mvnw -Dtest=RulesBehaviourTest \
  -Dcucumber.features=src/test/resources/features/rules/kiosk_scope.feature test          # one feature
```

The eight campaigns feature files get the extra feature-level tag `@KIO-24` (a one-line change each, no scenario changes), and the convention of `AGENTS.md` becomes: tag each feature with its task code and each scenario with the requirement ids.

### 5.2 Unit tests (technical details and exact boundaries)

All named `*Test`, no Spring, database or network; run with `./mvnw test`. Scenarios hold the representative business cases; unit tests hold what would bloat the scenarios (R11.7).

| Test class | Covers |
|---|---|
| `valueobject/DailySlotTest`, `ScheduleWindowTest`, `KioskSelectionTest`, `KioskIdTest` | construction rules, `crossesMidnight`, exact id comparison with a different case (R6.3), defensive copies |
| `service/AvailabilityEvaluatorTest` | every status; the order of reasons when several hold (R8.2); the exact boundaries 23:59:59 / 00:00:00 for dates, days and slots; slots of seconds and nanoseconds; crossing slot in the three parts of the day; the table of 3.2; no mutation of the inputs; same inputs, same answer (R8.4) |
| `application/CampaignRunPlansTest` | mapping of every field and of every status (exhaustive); scope with one and with several kiosks |
| `application/FindCandidateCampaignsTest` | the use case with `Clock.fixed` and a fake catalog: statuses queried and filtered, order kept (active first), other store ignored (R1.5), empty result, blank arguments; the **same instant in two zones** gives different local dates (R3.5, R7.3); the two daylight saving days of 3.3 (R7.4); the moment is read once per call; the catalog is never written to |

Test helpers: `CampaignViewBuilder` (for example `anActiveCampaign().runningOn(MONDAY).from("22:00").to("02:00")`) and `InMemoryCampaignCatalog`, used by scenarios and unit tests alike.

### 5.3 Traceability: where each requirement is verified (R11.7)

| Requirement | Scenarios | Unit tests |
|---|---|---|
| R1 candidates | `candidate_campaigns` (order, store, empty) | `FindCandidateCampaignsTest` (read-only, blank input, the moment is read once) |
| R2 statuses | `campaign_status` | `AvailabilityEvaluatorTest` (all five statuses) |
| R3 dates | `schedule_dates` (start, end, open ended) | `AvailabilityEvaluatorTest` (boundaries at midnight), `FindCandidateCampaignsTest` (R3.5, two zones) |
| R4 days | `schedule_days` | `AvailabilityEvaluatorTest` (day change) |
| R5 slot | `schedule_time_slot` (incl. crossing midnight table) | `AvailabilityEvaluatorTest` (seconds, nanoseconds, gap of the day), `DailySlotTest` |
| R6 scope | `kiosk_scope` | `KioskSelectionTest`, `KioskIdTest` (case) |
| R7 clock and zone | `candidate_campaigns` (zones) | `FindCandidateCampaignsTest` (two zones, both daylight saving days), architecture rule R7.2 |
| R8 reasons | `candidate_campaigns` (one reason shown) | `AvailabilityEvaluatorTest` (order), determinism |
| R9 structure | — | `ArchitectureTest`, `ModularityTest` |
| R13.1, R13.4 | — | review plus `ArchitectureTest` (no network, no storage classes in the module) |

### 5.4 Architecture and module tests

New rules in `ArchitectureRules`, each with a fixture that breaks it under `com.cvretail.archfixtures` and a case in `ArchitectureRulesDetectViolationsTest` that asserts the build fails naming the class:

- `NO_SYSTEM_CLOCK_ACCESS` (R7.2), for all production code: no class calls the no-argument `now()` of `Instant`, `LocalDate`, `LocalTime`, `LocalDateTime`, `ZonedDateTime`, `OffsetDateTime`, nor `ZoneId.systemDefault()`, `TimeZone.getDefault()` or `System.currentTimeMillis()`. Written with `noClasses().should().callMethod(Instant.class, "now")` and the same for the others (the overloads with a `Clock` are allowed).
- `domainIsIndependentOfOtherModules(module, others...)` (R9.1): one instance each for `campaigns`, `rules` and `reports`, so no `domain` package depends on another module.
- The existing rules (`DOMAIN_IS_FRAMEWORK_FREE`, `APPLICATION_DOES_NOT_USE_INFRASTRUCTURE`, `INFRASTRUCTURE_IS_A_LEAF`, `PUBLISHED_SURFACE_IS_SELF_CONTAINED`, `DOMAIN_INTERNAL_LAYERING`) now also cover real `rules` classes.
- `ModularityTest.moduleStructureIsValid` (`verify()`) fails if `rules` touches a non-exposed type of `campaigns`. A new assertion checks that `rules` exposes nothing yet (R9.5).

## 6. Documentation (R12)

- `services/core-backend/README.md`: a "Rules engine: schedule and scope" section (the use case, the rules of dates, days, slot and scope, the schedule-day rule with the table of 3.2, daylight saving behavior, how time and zone are obtained and that KIO-62 provides them), and the BDD section extended with the rules commands and the `@KIO-n and @Rn` tag form.
- `AGENTS.md`: the project status line; the tag convention (feature tag with the task code, scenario tags with requirement ids).
- The Trello card KIO-25 is updated at the end.

## 7. Verification strategy

- `./mvnw test` runs both Cucumber suites and all unit tests; `./mvnw -B -ntp verify` passes.
- Both HTML reports exist after a run: `target/cucumber-reports/campaigns.html` and `rules.html`.
- The selection commands of 5.1 work with Surefire (one suite, one tag with the task code, one feature).
- **Negative checks**, then undone:
  - make the slot end inclusive: a scenario and `AvailabilityEvaluatorTest` fail;
  - remove the schedule-day rule (step 3): the crossing-midnight scenarios fail;
  - convert the instant with the UTC date instead of the clock zone: the two-zone test fails;
  - call `Instant.now()` in `rules.application`: `NO_SYSTEM_CLOCK_ACCESS` fails;
  - import a `campaigns.domain` class in `rules.domain`: the independence rule and `ModularityTest` fail;
  - ask the catalog only for `ACTIVE`: the `SCHEDULED` scenario fails.
- `git diff --stat` shows no change in `contracts/`, compose, `.env.example`, CI, migrations, runtime dependencies or the production code of `campaigns` (the feature files and README aside).

## 8. Requirement coverage

| Requirement | Covered by |
|---|---|
| R1 | 4 (`candidatesFor`), 5.1 `candidate_campaigns`, 5.2 |
| R2 | 3.1 `CampaignLifecycle`, 3.2 step 1, 4 (two catalog calls), `campaign_status` |
| R3 | 3.2 steps 4–5, `schedule_dates` |
| R4 | 3.2 step 6, `schedule_days` |
| R5 | 3.2 steps 3 and 7, table of 3.2, `schedule_time_slot` |
| R6 | 3.1 `KioskSelection`, 3.2 step 8, `kiosk_scope` |
| R7 | 3.3, 4 (single `Clock`), 5.4 `NO_SYSTEM_CLOCK_ACCESS`, zone scenarios and tests |
| R8 | 3.1 `Availability`, 3.2 order of steps, 4 `availabilityOf` |
| R9 | 1 (structure), 2 (decisions 8 and 9), 5.4 |
| R10 | 5.2, 7 (fixed clocks only) |
| R11 | 5.1, 5.3 |
| R12 | 6 |
| R13 | 2 (no new dependency or configuration), 7 |

## 9. Differences from the requirements

- **R11.3 is extended:** besides the requirement ids, every feature carries the task tag (`@KIO-25`), and the KIO-24 features get `@KIO-24`, because requirement ids repeat across specs.
- **R8.3:** the reason is exposed by `availabilityOf` on the use case (internal to the module) and by `AvailabilityEvaluator`; the list returned by `candidatesFor` carries none.
- The input errors of the module are `IllegalArgumentException`, not a domain violation type; there is no `exception` sub-package in `rules.domain` yet.

## 10. Risks and open points

- **Nested types of the published surface:** `rules.application` uses `CampaignView.Status`, `Schedule`, `TimeSlot` and `Scope`, which are nested in an exposed type. `ModularityTest` must accept them as exposed; the first compile and test run confirms it (the task that adds the mapper runs `ModularityTest` right away).
- **Two catalog calls:** the order is `ACTIVE` then `SCHEDULED`, not a single global order. KIO-28 sorts by priority anyway; if a single query is needed, that is a change to the published surface of `campaigns` and goes through a spec update first.
- **Wall-clock semantics:** daylight saving days make a slot shorter or longer in real time (3.3). This is the normal expectation for store hours and is documented.
- **Tag convention change:** the `@KIO-24` line added to the eight campaigns features touches already merged files; the change is mechanical and the scenarios are not edited.
- **Cucumber report property:** moving the plugin from the shared properties file to the runners must not hide the campaigns report; the verification step checks both files exist.
- **Zone source:** until KIO-62, no real `Clock` bean exists, so the use case cannot be called from the running application yet. This is intended and stated in the README.
