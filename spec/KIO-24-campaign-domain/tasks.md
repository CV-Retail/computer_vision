# KIO-24 Campaign domain: tasks

Implements `design.md` for `requirements.md`. Do the tasks in order and tick each one when done. Commands run from `services/core-backend` unless a task says otherwise. Every task ends with `./mvnw test` green before the next one starts. Do not commit, push or open a PR until the user asks. If something in `design.md` turns out to be different when built, fix the spec first and tell the user. Changes to `contracts/`, `docker-compose.yml`, `.env.example` and the CI workflow are out of scope. Production classes live under `src/main/java/com/cvretail/kiosko/campaigns/`, tests under `src/test/java/com/cvretail/kiosko/campaigns/`.

## Tests in this feature

All tests are unit tests named `*Test` (no Spring context, database or network; run by `./mvnw test`), including the Cucumber suite. **No `*IT` integration test is added**: nothing here touches a database, HTTP or the bus; integration tests arrive with the persistence adapter (KIO-49). Each test is written in the same task as the code it covers.

| Task | Unit tests created |
|---|---|
| T1 | `CampaignDomainBehaviourTest` (Cucumber runner, throwaway scenario) |
| T2 | `IdentifiersTest` |
| T3 | `CampaignNameTest`, `MinimumOnScreenTimeTest`, `PlaylistTest`, `ScheduleTest`, `ScopeTest` |
| T4 | `CampaignSnapshotTest` |
| T5 | `CampaignStatusPolicyTest`, `CampaignInvariantsTest` |
| T6 | `CampaignEventsTest` |
| T7 | `CampaignTest` |
| T8 | `CampaignRepositoryContractTest` (abstract), `InMemoryCampaignRepositoryTest` |
| T9 | `CampaignViewsTest` |
| T10 | `ArchitectureTest` and `ArchitectureRulesDetectViolationsTest` (extended), `ModularityTest` (extended) |
| T11 | the eight `.feature` files run by `CampaignDomainBehaviourTest` |
| T12 | `ViolationCodesTest` |

## Tasks

- [x] **T1 Cucumber setup and spike check** (R18.3, R21.1, R21.7)
  In `pom.xml` add the property `cucumber.version` = `8.0.4`, import `io.cucumber:cucumber-bom` in `dependencyManagement`, and add as test scope `cucumber-java`, `cucumber-junit-platform-engine`, `cucumber-picocontainer` and `junit-platform-suite`. Add `src/test/resources/junit-platform.properties` (HTML report under `target/cucumber-reports`, no publish banner). Add a throwaway feature with one scenario, a steps class, and the `CampaignDomainBehaviourTest` runner from `design.md` section 5.1. Run `./mvnw dependency:tree -Dscope=test` and re-read the licenses of the new artifacts (including `junit-platform-suite`, which the design took from memory); correct `design.md` if any differs.
  *Verify:* `./mvnw test` runs the throwaway scenario with no warning; `./mvnw -Dtest=CampaignDomainBehaviourTest test` runs only it; confirm that `-Dcucumber.filter.tags="@x"` and `-Dcucumber.features=<path>` reach Cucumber through Surefire (if not, find the working form and update `design.md` sections 5.1 and 6); `git diff pom.xml` adds only the property, the BOM and the four test dependencies. Delete the throwaway feature and steps when T11 replaces them.

- [x] **T2 Exceptions and identifier value objects** (R1.2, R13, R19.1, R19.2)
  Create `domain/exception/Violation` and `CampaignViolationException` (non-empty list, English messages), a holder with every violation code as a constant, and in `domain/valueobject` the identifiers `CampaignId` (with `generate()`), `StoreId`, `KioskId`, `AudienceRuleId`, `VetoId` and `MediaRef`, with the rules and codes of `design.md` section 3.1. A `null` reports `<field>.required`.
  *Verify:* `valueobject/IdentifiersTest` covers valid and invalid values, equality and the codes; `./mvnw test` passes.

- [x] **T3 Content value objects** (R2, R5, R8.1, R6.1, R7.1)
  In `domain/valueobject`: `CampaignName` (trimmed, 1–100), `MinimumOnScreenTime` (1–3600, default 8), `PlaylistItem`, `Playlist` (at most 50 items; `add`, `insert`, `remove`, `move`, `withDuration`, zero-based, no gaps), `TimeSlot`, `Schedule` (with `always()`), and `Scope` (sealed `AllKiosks` | `SelectedKiosks`, built by `SelectedKiosks.of`).
  *Verify:* `CampaignNameTest`, `MinimumOnScreenTimeTest`, `PlaylistTest`, `ScheduleTest` and `ScopeTest` cover each bound and each code; a rejected `Playlist` operation leaves the original unchanged.

- [x] **T4 Enums and snapshot** (R1.3, R3, R12.5, R19.2)
  In `domain/valueobject`: `CampaignType`, `CampaignStatus`, `Priority` and the immutable `CampaignSnapshot` with its `with…` methods and `Optional` components.
  *Verify:* `CampaignSnapshotTest` checks the `with…` copies and that the original is unchanged; no `null` is accepted for a required component.

- [x] **T5 Domain services** (R3, R4, R8, R9, R10, R11, R12)
  In `domain/service`: `CampaignStatusPolicy` (the transition table of `design.md` section 3.5, codes `status.transition_not_allowed` and `status.finished`) and `CampaignInvariants` (`requireNotFinished`, the type rules of section 3.3, completeness of section 3.4; all violations reported together).
  *Verify:* `CampaignStatusPolicyTest` covers every status × transition pair; `CampaignInvariantsTest` covers each type rule and each completeness code, including several missing pieces in one exception.

- [x] **T6 Domain events** (R20.1, R20.2)
  In `domain/event`: the sealed `CampaignEvent` and the five event records, depending only on `valueobject`.
  *Verify:* it compiles; a small `CampaignEventsTest` checks the fields and that the sealed hierarchy lists exactly five types.

- [x] **T7 Aggregate** (R1, R3–R12, R20)
  In `domain/entity/Campaign`: the factories `createRegular`, `createDefault`, `restore`; the readers; `snapshot()`; `pullEvents()`; every mutator and the four transitions, all through the private `change(...)` of `design.md` section 3.2; `equals` and `hashCode` by `CampaignId`. Add the test helper `CampaignBuilder` and the unit test `entity/CampaignTest` for the technical edge cases that scenarios do not cover (restore of invalid states, equality, every mutator leaving state and events untouched when rejected).
  *Verify:* `CampaignTest` passes; a grep shows no assignment to the state field outside `change` and the factories (review check of the risk in `design.md` section 10).

- [x] **T8 Repository port and in-memory implementation** (R14)
  In `domain/repository/CampaignRepository` the port of section 3.8. In the test sources, `InMemoryCampaignRepository`, the abstract `CampaignRepositoryContractTest` and `InMemoryCampaignRepositoryTest` that extends it.
  *Verify:* the contract covers save and update, find by store, store isolation, default lookup and the delete result; `./mvnw test` passes.

- [x] **T9 Published surface** (R15, R16.1)
  In the base package: `CampaignView` and `CampaignCatalog`. In `application`: `CampaignViews.from(Campaign)` with exhaustive `switch` expressions.
  *Verify:* `application/CampaignViewsTest` maps every field for `REGULAR` and `DEFAULT` and checks enum name parity with the domain enums.

- [x] **T10 Architecture and module rules** (R15.2, R15.5, R16, R19.3–R19.5)
  In `ArchitectureRules` add `PUBLISHED_SURFACE_IS_SELF_CONTAINED` and `DOMAIN_INTERNAL_LAYERING`; run both from `ArchitectureTest`. Add fixtures under `com.cvretail.archfixtures` that break each rule and cases in `ArchitectureRulesDetectViolationsTest`. In `ModularityTest` assert with `ApplicationModule.isExposed` that `CampaignView` and `CampaignCatalog` are exposed and `Campaign` is not.
  *Verify:* `./mvnw test` passes; the negative checks of `design.md` section 7 for architecture (Spring import in the domain, a value object using an entity, the view using a domain type, `rules` using `Campaign`) each fail the build, then are undone.

- [x] **T11 Feature files and steps** (R21.2–R21.9, all behavior requirements)
  Remove the throwaway scenario of T1. Write the eight feature files of `design.md` section 5.1 in English and business language, tagged `@R..` per scenario, with the status × transition `Scenario Outline`. Write `CampaignWorld`, `RejectionReasons` and one steps class per feature area, using `CampaignBuilder`.
  *Verify:* `./mvnw test` runs all scenarios; every behavior requirement in the traceability table of `design.md` section 5.3 has at least one tagged scenario (check with a grep of the tags); the filters of T1 select one feature and one tag; the HTML report exists under `target/cucumber-reports`.

- [x] **T12 Violation catalog test and negative checks** (R13, R21.7)
  Add `exception/ViolationCodesTest`: every constant in the codes holder is raised by at least one unit test or scenario (collect the codes thrown during the suite, or assert each one explicitly). Then run the negative checks of `design.md` section 7 for behavior: remove the transition guard and confirm a scenario fails; remove one value-object bound and confirm a unit test fails. Undo each change.
  *Verify:* the checks fail as expected and the suite is green again after undoing them.

- [x] **T13 Documentation** (R17)
  `services/core-backend/README.md`: "Campaign domain" section (structure by building block, status diagram, type rules and completeness tables, published surface, error codes, glossary of Spanish business terms) and "Behavior-driven specification" section (where the features live, how to run all, one feature and one tag, how tags trace to requirements), in the language the README already uses. `AGENTS.md`: update the status line, note that domain code started with KIO-24, document the sub-package structure and the BDD convention (R17.5), and the commands to run scenarios.
  *Verify:* every command in the docs was run and works; links resolve.

- [x] **T14 Final verification and wrap-up** (R16, R18)
  Run `./mvnw -B -ntp verify`. Check `git status` and `git diff --stat`: no change in `contracts/`, no migration, no new runtime dependency, `pom.xml` adds only the Cucumber items. Tick all tasks. Record results in "Verification results". Update the Trello card KIO-24 (description and list) as the final step.
  *Verify:* `verify` passes; the diff matches the list above; list the files changed in the summary to the user.

## Verification results

Run on 2026-10-14, from `services/core-backend`.

- **T1:** `./mvnw dependency:tree -Dscope=test` resolves Cucumber 8.0.4 (`cucumber-java`, `cucumber-junit-platform-engine`, `cucumber-picocontainer`), PicoContainer 2.16 and `junit-platform-suite` 6.0.3, with no warning. `-Dcucumber.filter.tags="@nomatch"` skips the scenarios and `-Dcucumber.features=<path>` selects a feature, both through Surefire. Licenses: `junit-platform-suite` is EPL-2.0 (its POM). Cucumber-JVM MIT and PicoContainer BSD were taken from upstream sources earlier. `pom.xml` adds one property, the Cucumber BOM and four test dependencies.
- **T2–T9:** value objects, services, events, aggregate, repository port, in-memory repository and published surface compile and pass their unit tests. The reader of the schedule is `runSchedule()` because `schedule()` is the transition (recorded in `design.md` section 9, with the codes added and dropped).
- **T10:** `PUBLISHED_SURFACE_IS_SELF_CONTAINED` and `DOMAIN_INTERNAL_LAYERING` pass on production classes and fail on the new fixtures; `ModularityTest` confirms `CampaignView` and `CampaignCatalog` are exposed and `Campaign` is not. Negative checks: a Spring import in the domain, a value object using the aggregate, `CampaignCatalog` returning a domain type and a `rules` class using `Campaign` each fail the build naming the problem; all were undone.
- **T11:** 8 feature files, 78 test runs (outline rows expanded), all passing. Every behavior requirement R1–R12 and R20 has at least one tag. `-Dcucumber.filter.tags="@R11"` ran only the `@R11` scenarios and skipped the rest; `-Dcucumber.features=…/playlist.feature` ran only that file. The HTML report is `target/cucumber-reports/campaigns.html`.
- **T12:** `ViolationCodesTest` ties every code of `ViolationCodes` to a real action. Negative checks: disabling the transition guard failed several scenarios (the transition matrix and the rejected-transition scenarios); widening the minimum time bound failed `MinimumOnScreenTimeTest` and `ViolationCodesTest`. Both were undone.
- **T13:** `services/core-backend/README.md` (Campaign domain, glossary, Behavior-driven specification) and `AGENTS.md` (status, sub-packages, BDD convention, commands) updated.
- **T14:** `./mvnw -B -ntp verify`: 186 unit tests and 3 integration tests, BUILD SUCCESS. No change in `contracts/`, `docker-compose.yml`, `.env.example` or `.github/`; no migration; no runtime dependency.
