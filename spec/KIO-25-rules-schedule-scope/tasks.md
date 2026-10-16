# KIO-25 Rules: schedule and scope: tasks

Implements `design.md` for `requirements.md`. Do the tasks in order and tick each one when done. Commands run from `services/core-backend` unless a task says otherwise. Every task ends with `./mvnw test` green before the next one starts. Do not commit, push or open a PR until the user asks. If something in `design.md` turns out to be different when built, fix the spec first and tell the user. Changes to `contracts/`, `docker-compose.yml`, `.env.example`, the CI workflow, migrations and the production code of `campaigns` are out of scope. Production classes live under `src/main/java/com/cvretail/kiosko/rules/`, tests under `src/test/java/com/cvretail/kiosko/rules/`.

## Tests in this feature

All tests are unit tests named `*Test` (no Spring context, database or network; run by `./mvnw test`), including the Cucumber suite. **No `*IT` integration test is added**: nothing here touches a database, HTTP or the bus. Each test is written in the same task as the code it covers. Every test uses a fixed clock or a local date and time given by the test, never the real time.

| Task | Unit tests created or extended |
|---|---|
| T2 | `DailySlotTest`, `ScheduleWindowTest`, `KioskSelectionTest`, `KioskIdTest` |
| T3 | `AvailabilityEvaluatorTest` |
| T4 | `CampaignRunPlansTest` (and `ModularityTest` re-run) |
| T5 | `FindCandidateCampaignsTest` |
| T6 | `ArchitectureTest`, `ArchitectureRulesDetectViolationsTest`, `ModularityTest` (extended) |
| T7–T8 | `RulesBehaviourTest` (the six `.feature` files) |

## Tasks

- [x] **T1 Tag convention and report files of the Cucumber suites** (R11.6, design 5.1 and 9)
  Remove `cucumber.plugin` from `src/test/resources/junit-platform.properties` (keep `cucumber.publish.quiet`). Add `@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "html:target/cucumber-reports/campaigns.html")` to `CampaignDomainBehaviourTest`. Add the feature-level tag `@KIO-24` as the first line of each of the eight files in `src/test/resources/features/campaigns/` (a tag line on its own above the existing `@R..` line; no scenario changes).
  *Verify:* `./mvnw test` passes with the same number of tests as before (78 scenario runs); `target/cucumber-reports/campaigns.html` exists; `-Dcucumber.filter.tags="@KIO-24 and @R11"` runs only the lifecycle scenarios and skips the rest; `git diff` of the feature files shows one added line each.

- [x] **T2 Domain value objects** (R2.1, R3, R4, R5, R6, R8.1, R9.1, R9.3)
  In `rules/domain/valueobject`: `CampaignLifecycle` (with `isEligible()`), `DailySlot`, `ScheduleWindow`, `KioskId`, `KioskSelection` (sealed, `includes`), `CampaignRunPlan`, `Availability` (with `applies()`), as in design 3.1. Inconsistent data throws `IllegalArgumentException`. Add a `package-info.java` to the new sub-packages. The domain imports nothing but the JDK.
  *Verify:* `DailySlotTest`, `ScheduleWindowTest`, `KioskSelectionTest` and `KioskIdTest` cover valid and invalid values, `crossesMidnight`, `isEligible` for the five statuses, exact id comparison with a different case, defensive copies of the sets; `ArchitectureTest` still passes.

- [x] **T3 Availability evaluator** (R2–R6, R8.1, R8.2, R8.4)
  In `rules/domain/service/AvailabilityEvaluator`: the nine steps of design 3.2, a pure static function of the plan, the kiosk and a `LocalDateTime`.
  *Verify:* `AvailabilityEvaluatorTest` covers every status; start and end date before, on and after; each day of the week; non-crossing slot at start, one second before the end, at the end; the crossing-midnight table of design 3.2 (all seven rows); 23:59:59 and 00:00:00; a time in the gap of a crossing slot; kiosk included and excluded; the order of reasons when several hold; the same inputs always give the same answer. A temporary change that makes the slot end inclusive makes at least one test fail (undo it).

- [x] **T4 Mapping of the published view and test support** (R9.2, R9.4, design 2 decision 8 and 10)
  In `rules/application/CampaignRunPlans` (package-private): `from(CampaignView)` with an exhaustive `switch` over `CampaignView.Status`, the schedule and the scope. In the test sources: `support/CampaignViewBuilder` and `support/InMemoryCampaignCatalog` (a fake of `CampaignCatalog` that returns the views it holds by store and status, in insertion order).
  *Verify:* `CampaignRunPlansTest` maps every field and every status, a scope with one and with several kiosks, and a slot that crosses midnight. **Run `ModularityTest` right now**: it must still pass, which confirms that the nested types of `CampaignView` count as exposed (the risk of design 10). If it fails, stop and update the spec before continuing.

- [x] **T5 Use case: candidates for a kiosk now** (R1, R2.3, R7, R8.3)
  In `rules/application/FindCandidateCampaigns`: the constructor `(CampaignCatalog, Clock)`, `candidatesFor(storeId, kioskId)` and `availabilityOf(view, kioskId)` as in design 4. The moment is read once with `ZonedDateTime.now(clock).toLocalDateTime()`. Blank arguments throw `IllegalArgumentException`. No Spring annotation.
  *Verify:* `FindCandidateCampaignsTest` with `Clock.fixed` and the fake catalog: `DRAFT`, `PAUSED` and `FINISHED` excluded, `SCHEDULED` included; `ACTIVE` candidates come before `SCHEDULED` ones and each group keeps the catalog order; a view of another store is ignored; empty result; blank arguments rejected; the same instant in `Europe/Madrid` and in `America/Los_Angeles` gives different local dates and different answers; 2030-03-31 and 2030-10-27 in `Europe/Madrid` behave as in design 3.3 (slot `02:30–02:45` never applies on the first day; the repeated 02:30 gives the same answer twice on the second); the catalog was only read.

- [x] **T6 Architecture rules** (R7.2, R9.1, R9.5)
  In `ArchitectureRules` add `NO_SYSTEM_CLOCK_ACCESS` (no-argument `now()` of `Instant`, `LocalDate`, `LocalTime`, `LocalDateTime`, `ZonedDateTime`, `OffsetDateTime`, and `ZoneId.systemDefault()`, `TimeZone.getDefault()`, `System.currentTimeMillis()`) and `domainIsIndependentOfOtherModules(module, others...)` with an instance for each of `campaigns`, `rules` and `reports`. Run them from `ArchitectureTest`. Add fixtures under `com.cvretail.archfixtures` that break each rule and cases in `ArchitectureRulesDetectViolationsTest` that assert the failure names the class. Add to `ModularityTest` an assertion that `rules` exposes no type of its base package other than `package-info` (R9.5).
  *Verify:* `./mvnw test` passes; the fixtures fail the rules; the production code, including `campaigns` and the new code, passes `NO_SYSTEM_CLOCK_ACCESS`.

- [x] **T7 Cucumber suite for rules, first feature** (R11.1, R11.4, R11.6)
  Create `RulesBehaviourTest` (design 5.1) with `PLUGIN_PROPERTY_NAME` `html:target/cucumber-reports/rules.html`, `RulesWorld` (fixed clock and zone, store, kiosk, catalog, last result), the shared steps (store and kiosk, zone, "the server time is", "the campaign is a candidate / not a candidate for the kiosk", "the reason is …" mapped to `Availability`), and the first feature `campaign_status.feature` tagged `@KIO-25 @R2`.
  *Verify:* `./mvnw test` runs the new scenarios and the campaigns ones; both `campaigns.html` and `rules.html` exist; `-Dtest=RulesBehaviourTest` runs only the rules suite; `-Dcucumber.filter.tags="@KIO-25 and @R2"` selects the new scenarios and nothing from KIO-24.

- [x] **T8 Remaining features** (R1, R3–R8, R11.2, R11.3, R11.5)
  Write `schedule_dates.feature` (`@R3`), `schedule_days.feature` (`@R4`), `schedule_time_slot.feature` (`@R5`; outlines for moments around a slot from 09:00 to 18:00 and for the crossing-midnight table of design 3.2), `kiosk_scope.feature` (`@R6`) and `candidate_campaigns.feature` (`@R1 @R7 @R8`: active before scheduled, other store ignored, empty result, the same moment in two zones, one reason shown). Every feature carries `@KIO-25`, every scenario its requirement ids, in business words. Add the steps for schedules and scope (`ScheduleSteps`, `ScopeSteps`, `CandidateSteps`).
  *Verify:* `./mvnw test` passes; a grep of the tags shows at least one scenario for each of R1 to R8 and no scenario that duplicates a boundary already covered in `AvailabilityEvaluatorTest` (design 5.3); the selection commands of design 5.1 (suite, tag with task code, one feature file) work through Surefire.

- [x] **T9 Negative checks** (design 7)
  Run each check, confirm it fails the build, and undo it: make the slot end inclusive; remove the schedule-day rule (step 3 of the algorithm); convert the instant with the UTC date; call `Instant.now()` in `rules.application`; import a `campaigns.domain` class in `rules.domain`; ask the catalog only for `ACTIVE`.
  *Verify:* each check fails with a test or rule that names the problem; after undoing all of them `./mvnw test` is green and `git status` shows only intended changes.

- [x] **T10 Documentation** (R12)
  `services/core-backend/README.md`: the "Rules engine: schedule and scope" section (the use case, dates, days, slot, scope, the schedule-day rule with its table, daylight saving behavior, how time and zone are obtained and that KIO-62 provides them) and the commands of design 5.1 in the BDD section, with the `@KIO-n and @Rn` form. `AGENTS.md`: update the status line and the tag convention (feature tag with the task code, scenario tags with the requirement ids) and add the rules scenario commands to the CI line.
  *Verify:* every command in the docs was run and works; links resolve.

- [x] **T11 Final verification and wrap-up** (R10.5, R13)
  Run `./mvnw -B -ntp verify`. Check `git status` and `git diff --stat`: no change in `contracts/`, compose, `.env.example`, `.github/`, migrations, runtime dependencies or the production code of `campaigns`. Tick all tasks and record the results in "Verification results". Update the Trello card KIO-25 as the final step.
  *Verify:* `verify` passes; the diff matches the list above; list the files changed in the summary to the user.

## Verification results

Run on 2026-10-16, from `services/core-backend`.

- **T1:** `junit-platform.properties` keeps only `cucumber.publish.quiet`; each runner sets its own HTML report. `@KIO-24` added to the eight campaigns features (8 insertions, no other change). 186 tests as before; `-Dcucumber.filter.tags="@KIO-24 and @R11"` runs only the lifecycle scenarios.
- **T2–T3:** value objects and `AvailabilityEvaluator` with their unit tests (223 tests in total at that point). The crossing-midnight table of design 3.2 is asserted row by row.
- **T4:** `CampaignRunPlans`, `CampaignViewBuilder` and `InMemoryCampaignCatalog`. `ModularityTest` passed right after the mapper compiled, so the nested types of `CampaignView` count as exposed (the risk of design 10 did not materialize).
- **T5:** `FindCandidateCampaigns` and `FindCandidateCampaignsTest` (two zones, both Madrid daylight saving days, order, other store, blank arguments, the clock is read once, the catalog is only read).
- **T6:** `NO_SYSTEM_CLOCK_ACCESS`, `domainIsIndependentOf` (instances for campaigns, rules and reports) and a test that `rules` publishes nothing; fixtures prove each rule fails and that the overloads taking a `Clock` are allowed. Needed `rawParameterTypes(new Class<?>[0])` because the ArchUnit overload is ambiguous without a type.
- **T7–T8:** `RulesBehaviourTest` with six features: 51 test runs; `target/cucumber-reports/` has both `campaigns.html` and `rules.html`. Tags cover R1 to R8 (R7.3 and R7.4 with the zone scenarios). `-Dcucumber.filter.tags="@KIO-25 and @R5"` ran the `@R5` scenarios and skipped the rest (29 selected, 14 skipped); `-Dcucumber.features=…/kiosk_scope.feature` ran 4; the campaigns runner with `@KIO-25` skips everything.
- **T9:** each of the six negative checks failed the build and was undone: inclusive slot end (evaluator, use case and scenarios), no schedule-day rule (evaluator and scenarios), UTC date instead of the zone (use case and scenarios), `Instant.now()` in `rules.application` (`NO_SYSTEM_CLOCK_ACCESS`), a `campaigns.domain` class in `rules.domain` (independence rule and `ModularityTest`), only `ACTIVE` asked (use case and scenarios).
- **T10:** README (new "Rules engine: schedule and scope" section, two-suite table, tag form `@KIO-n and @Rn`) and `AGENTS.md` (status, tag convention, no system clock, commands). Every command in them was run.
- **T11:** `./mvnw -B -ntp verify`: 300 unit tests (51 rules scenarios and 78 campaigns scenarios among them) and 3 integration tests, BUILD SUCCESS. No change in `contracts/`, compose, `.env.example`, `.github/`, migrations, `pom.xml` or any production class of `campaigns`.
