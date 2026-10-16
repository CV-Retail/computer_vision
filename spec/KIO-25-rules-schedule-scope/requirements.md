# KIO-25 Rules: schedule and scope: requirements

## Introduction

The rules engine decides what the kiosk plays in a fixed order: **1. schedule and scope**, 2. vetoes, 3. audience, 4. priority and weight, 5. default playlist (`docs/kiosko-arquitectura-mvp.md`, "Motor de reglas"). This feature builds the first step in the `rules` module of `core-backend`: given a store, a kiosk and the current moment, it answers **which campaigns are candidates because their calendar (dates, days of the week, time slot) and their scope (all kiosks or only some) allow them to run now**. The later steps (KIO-26 to KIO-28) receive this list and narrow it further.

The data it reads already exists: the campaign domain (KIO-24) records the schedule and the scope of every campaign and publishes them through the read-only `CampaignView` and the query port `CampaignCatalog` in the base package of `campaigns`. The decision is deterministic and uses no AI. The current time comes from an injected clock, so tests can fix it; the time zone is the one of the server (its source is KIO-62).

Goals:

- A pure, easily tested evaluation of "does this schedule apply at this moment" and "does this scope include this kiosk", with all the edge cases of time (change of day, slots that cross midnight, inclusive dates, daylight saving time).
- A use case that gets the campaigns of a store from the published surface of `campaigns` and returns the candidates for one kiosk now.
- The `rules` module stays independent from the internals of `campaigns` and keeps its own domain free of any dependency on it.

Decisions taken for this feature (to confirm in review; see open questions):

1. **Only the schedule and the scope are decided here.** Vetoes, audience, priority, minimum on-screen time and the default playlist are other features (KIO-26 to KIO-28).
2. **Time comes from an injected `java.time.Clock`.** The moment is `clock.instant()` and the time zone is `clock.getZone()`. No class reads the system clock or the system zone by itself. How the server zone is configured, shown and checked is KIO-62; this feature does not add configuration, a Spring bean or an environment variable.
3. **The `rules` domain does not depend on `campaigns`.** The application layer maps `CampaignView` to the small types the domain needs.
4. **Business behavior is specified as Gherkin scenarios in English** (convention of `AGENTS.md`), with a simulated clock, plus unit tests for the technical details.

Source: Trello card KIO-25, `docs/kiosko-arquitectura-mvp.md` (Motor de reglas), the administration document (concepts Campaña, Programación and Alcance; "Hora y zona horaria del servidor"), `spec/KIO-24-campaign-domain/` and `AGENTS.md`.

## Requirements

### R1 Candidates for a kiosk now

**User story:** As the rules engine, I want the campaigns of a store that may run on a given kiosk at this moment so that the next steps only evaluate campaigns that are allowed to play.

Acceptance criteria:

- R1.1 The system shall provide a use case that, given a store id and a kiosk id, returns the campaigns of that store that are candidates for that kiosk at the current moment of the clock.
- R1.2 The use case shall read the campaigns only through the published surface of the `campaigns` module (`CampaignCatalog` and `CampaignView`).
- R1.3 A campaign shall be a candidate if and only if its status is eligible (R2), its schedule applies at the current moment (R3 to R5) and its scope includes the kiosk (R6).
- R1.4 The candidates shall keep the order in which the catalog returned the campaigns.
- R1.5 The use case shall return only campaigns of the requested store.
- R1.6 If the store has no eligible campaign, then the use case shall return an empty list and shall not fail.
- R1.7 The use case shall not change any campaign and shall not store anything.

### R2 Eligible statuses

**User story:** As an administrator, I want drafts, paused and finished campaigns to be ignored so that only campaigns I have released can play.

Acceptance criteria:

- R2.1 A campaign in status `DRAFT`, `PAUSED` or `FINISHED` shall not be a candidate, whatever its schedule and scope.
- R2.2 A campaign in status `ACTIVE` shall be evaluated by its schedule and scope.
- R2.3 A campaign in status `SCHEDULED` shall be evaluated by its schedule and scope in the same way, so that the engine does not depend on when the job that moves campaigns from `SCHEDULED` to `ACTIVE` (KIO-54) runs.
- R2.4 The evaluation of the schedule and the scope shall not depend on the type of the campaign; choosing between `REGULAR` campaigns and the `DEFAULT` one is the job of KIO-28.

### R3 Dates

**User story:** As an administrator, I want a campaign to run only between its start and end dates.

Acceptance criteria:

- R3.1 If the schedule has a start date, then the campaign shall not apply on any day before it.
- R3.2 If the schedule has an end date, then the campaign shall not apply on any day after it; the end date itself is included until its last moment.
- R3.3 If the schedule has no start date, then the campaign shall have no lower limit; if it has no end date, then it shall have no upper limit.
- R3.4 The start date shall be included from its first moment (00:00 local time).
- R3.5 The dates shall be compared with the local date of the current moment in the time zone of the clock (R7), never with the date in UTC.

### R4 Days of the week

**User story:** As an administrator, I want a campaign to run only on the days I chose.

Acceptance criteria:

- R4.1 The campaign shall apply only if the day of the week of the current local date is in the days of its schedule.
- R4.2 A schedule with all seven days shall impose no restriction.
- R4.3 The day of the week shall be that of the local date in the time zone of the clock (R7), except for the part of a slot that crosses midnight (R5.4).

### R5 Time slot

**User story:** As an administrator, I want a campaign to run only inside a time window of the day, including windows that cross midnight.

Acceptance criteria:

- R5.1 If the schedule has no time slot, then the campaign shall apply during the whole day.
- R5.2 If the slot start is before its end, then the campaign shall apply from the start time (included) until the end time (excluded) of the local time.
- R5.3 The end time of a slot shall not belong to the slot: a campaign with the slot 09:00 to 18:00 applies at 09:00:00 and does not apply at 18:00:00.
- R5.4 If the slot start is later than its end (a slot that crosses midnight), then the campaign shall apply from the start time until midnight of the day on which the slot starts and from midnight until the end time of the next day. The part after midnight shall belong to the **schedule day** on which the slot started: the days of the week (R4), the start date (R3.1) and the end date (R3.2) shall be checked against that schedule day, and not against the calendar day of the current moment.
- R5.5 The slot times shall be compared with the local time of the current moment in the time zone of the clock (R7).

### R6 Scope

**User story:** As an administrator, I want a campaign to run on all the kiosks of the store or only on the ones I chose.

Acceptance criteria:

- R6.1 If the scope is all kiosks, then the campaign shall include every kiosk of the store.
- R6.2 If the scope lists kiosks, then the campaign shall include a kiosk if and only if its id is in the list.
- R6.3 The comparison of kiosk ids shall be exact (same text, case sensitive).
- R6.4 The use case shall not check that the kiosk exists; that belongs to the kiosk configuration (KIO-57).

### R7 Server clock and time zone

**User story:** As a store owner, I want the schedules to follow the local time of the server so that "08:00 to 20:00" means the hours of my store.

Acceptance criteria:

- R7.1 The current moment and the time zone shall come from an injected `java.time.Clock`: the moment from `clock.instant()` and the zone from `clock.getZone()`.
- R7.2 No class of the feature shall call `Instant.now()`, `LocalDateTime.now()`, `ZoneId.systemDefault()` or any other way of reading the system clock or the default zone.
- R7.3 The same schedule shall give different results when the clock has a different time zone and the same instant.
- R7.4 On the days when the local clock moves forward or backward (daylight saving time), the evaluation shall use the local date and time of the instant in the zone, so that every instant has exactly one answer, and it shall not fail.
- R7.5 The feature shall not decide where the zone is configured, how it is shown or how a wrong server clock is warned; those are KIO-62. The feature shall not add a Spring bean, a configuration property or an environment variable.

### R8 Result and reasons

**User story:** As a developer of the next steps and of the validation views, I want to know why a campaign is not a candidate so that the decision can be explained and tested.

Acceptance criteria:

- R8.1 The evaluation of one campaign shall give one answer: it applies, or it does not apply for one reason from this list: status not eligible, before the start date, after the end date, day of the week not included, outside the time slot, kiosk not included.
- R8.2 If several reasons hold, then the answer shall be the first one in the order of R8.1.
- R8.3 The use case shall expose the list of candidates; the reasons are available to the code that evaluates one campaign and to the tests, and shall not be part of the result sent to the player.
- R8.4 The evaluation shall be deterministic: the same campaigns, kiosk, moment and zone shall always give the same answer.

### R9 Module boundaries and structure

**User story:** As the architect, I want the module boundaries of KIO-7 and the building blocks of KIO-24 respected.

Acceptance criteria:

- R9.1 The `domain` package of `rules` shall depend on nothing but the Java standard library and its own packages; in particular it shall not depend on any type of the `campaigns` module.
- R9.2 The `application` package of `rules` shall map `CampaignView` to the types of the `rules` domain, and shall use no internal package of `campaigns`.
- R9.3 The domain code of `rules` shall be organized in the sub-packages of `AGENTS.md` that it needs (`valueobject`, `service`, `exception` and the others only if used), following the dependency rules enforced by the architecture tests.
- R9.4 This feature shall not change the published surface of `campaigns`; if `CampaignView` lacks data, the spec shall be updated first (see open question 8).
- R9.5 The published surface of `rules` (its base package) shall contain only what other modules need; if nobody needs the use case yet, it shall stay internal.

### R10 Tests

**User story:** As the architect, I want the time rules proven with a simulated clock so that no test depends on the real time.

Acceptance criteria:

- R10.1 All tests shall use a fixed clock (`Clock.fixed`) with an explicit zone and shall not depend on the real time or on the machine zone.
- R10.2 There shall be unit tests, without Spring, database or network, for at least these cases: a campaign before, on and after the start date; before, on and after the end date; each day of the week included and excluded; a slot start and end (included and excluded); a slot that crosses midnight, before and after midnight, with the day of the week and the dates checked against the schedule day; a change of day at 23:59:59 and 00:00:00; the same instant in two zones that give different local dates; a day when the clock moves forward and a day when it moves backward.
- R10.3 There shall be unit tests for the scope: all kiosks, a listed kiosk, a not listed kiosk, a different case of the id.
- R10.4 There shall be unit tests for the use case, with a fake `CampaignCatalog`: status filtering, order kept, other store ignored, empty result.
- R10.5 `./mvnw -B -ntp verify` shall pass, including the module verification and the architecture rules, and the CI shall stay green without changing the workflow.
- R10.6 A test shall fail if a rule is removed (for example the exclusion of the end time of a slot).

### R11 Behavior-driven specification

**User story:** As a developer and as the business owner, I want the schedule and scope rules written as readable scenarios that also run as tests.

Acceptance criteria:

- R11.1 The behavior of R1 to R7 shall be specified as Gherkin scenarios in English under `src/test/resources/features/rules/`, with the steps in a `bdd` test package of the `rules` module, following the convention of `AGENTS.md`.
- R11.2 The scenarios shall use business words (campaign, kiosk, store, active, paused, start date, end date, days, time slot, "the server time is Monday 14 January 2030 at 23:30 in Madrid") and shall not mention Java classes or exceptions.
- R11.3 Each scenario shall carry a tag with the requirement ids it verifies (for example `@R5`).
- R11.4 The scenarios shall run through the JUnit Platform with `./mvnw test` (a suite class named `*Test`), without Spring, a database or a network, and shall use the simulated clock of R10.1.
- R11.5 Scenario outlines shall be used for the tables of cases (for example moments around the end of a slot).
- R11.6 The reports of the rules scenarios shall not overwrite the reports of the campaigns scenarios, and the two suites shall keep running together and separately by tag or feature.
- R11.7 The design shall contain the table that shows which requirement is verified by a scenario and which by a unit test, without duplicating a rule at both levels.

### R12 Documentation

**User story:** As a developer, I want a short description of how the first step of the engine works.

Acceptance criteria:

- R12.1 `services/core-backend/README.md` shall describe the first step of the engine: the use case, the rules of dates, days, slot (including the schedule-day rule for slots that cross midnight) and scope, and how the time and the zone are obtained.
- R12.2 The README shall explain how to run the rules scenarios, a single feature and a single tag.
- R12.3 `AGENTS.md` shall update the project status line in the same pull request.
- R12.4 The Trello card shall be updated at the end with what was delivered and what remains (not part of the code).

### R13 Constraints carried over

**User story:** As the project owner, I want the privacy, language and layering rules to hold.

Acceptance criteria:

- R13.1 The feature shall handle no personal data and no audience data; it only uses campaign data, a kiosk id and the time.
- R13.2 Identifiers, comments and messages in the code shall be in English.
- R13.3 The feature shall add no runtime dependency, no database migration and no change to `contracts/`, `docker-compose.yml` or `.env.example`; test dependencies are already present.
- R13.4 The feature shall not publish events, read the network, open a connection or write to a store.

## Out of scope

- Vetoes by content tag (KIO-26), audience conditions (KIO-27), priority, weight, tie-break, minimum on-screen time and the default playlist (KIO-28).
- The persistence adapter that implements `CampaignCatalog` (KIO-49) and the Spring wiring of the use case.
- Configuring, showing and warning about the server time and time zone, and the Spring `Clock` bean (KIO-62).
- Kiosk configuration and checking that a kiosk exists (KIO-57).
- The order in which the engine calls its steps, the WebSocket order to the player and the player itself (KIO-28 and later).
- The weekly coverage calendar and the conflict warnings (Phase 1, KIO-55).
- Import and export of schedules (KIO-30).
- Holidays, exceptions by date, and per-kiosk time zones.

## Open questions (to settle before the design)

1. **Statuses:** this spec evaluates `ACTIVE` and `SCHEDULED` (R2.3) and ignores the others. The alternative is `ACTIVE` only, relying on KIO-54 to flip `SCHEDULED` by date. Proposal: both, so the engine does not depend on a job.
2. **Slots that cross midnight:** R5.4 attributes the part after midnight to the day on which the slot started, for days of the week and for the end date. The alternative is to use the calendar day of the moment (a campaign "Monday 22:00 to 02:00" would then play on Monday 00:00 to 02:00 and Monday 22:00 to 24:00, and not on Tuesday 00:00 to 02:00). Proposal: the schedule-day rule, because it matches what an administrator means by "Monday, 22:00 to 02:00".
3. **End of a slot:** R5.3 excludes the end time. The alternative is to include the last second. Proposal: exclude it.
4. **Type of campaign:** R2.4 applies the same filter to `REGULAR` and `DEFAULT` campaigns and leaves the choice to KIO-28. The alternative is to skip `DEFAULT` here.
5. **Where the zone comes from until KIO-62:** the feature only uses the zone of the injected clock. A later feature supplies the real `Clock` bean. Confirm that this feature should not add a temporary bean or property.
6. **Reasons:** R8 returns reasons only for code and tests. The alternative is to expose them in the result for the future calendar and validation views (KIO-55).
7. **Second Cucumber suite:** a second runner and glue package for the `rules` module, and a report file per suite (R11.6). The design settles the names.
8. **Data in `CampaignView`:** it already has status, schedule and scope, so no change is expected. Confirm during design.
9. **Where the use case lives:** `rules.application` (internal) or exposed in the base package of `rules` for the later steps. Proposal: internal until KIO-28 needs it (R9.5).
