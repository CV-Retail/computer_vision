# KIO-24 Campaign domain: requirements

## Introduction

A **campaign** is the unit that is scheduled and shown on the kiosk: it joins a playlist, an audience rule, the vetoes that apply and a calendar (see `docs/Administración de campañas conceptos y vistas del administrador.md`, concept "Campaña", and the relation diagram on page 5 of the PDF). This feature builds the **domain** of the campaign in the `campaigns` module of `core-backend`: the aggregate, its value objects and invariants, its status transitions, the repository port, and a small read-only view that other modules may use. It is the first real domain code of the backend, written under the Clean Architecture and DDD rules of `AGENTS.md` ("Backend architecture"): no Spring, no persistence, no framework in `domain`.

Goals:

- Give the following features a stable core: persistence (KIO-49), the rules engine (KIO-25 to KIO-28), lifecycle use cases (KIO-54), validation warnings (KIO-55), the REST API (KIO-58) and import/export (KIO-30).
- Make every business rule about a campaign that does not depend on other aggregates explicit, small and tested with plain unit tests (`./mvnw test`, no Spring, no database).
- Make the architecture rules of KIO-7 meaningful: from now on `domain` contains real classes, so the ArchUnit rules stop being vacuous.

Decisions taken for this feature (user answers on 2026-10-14):

1. **Status transitions live in the aggregate now.** KIO-24 defines the statuses and the valid transitions as aggregate behavior; KIO-54 only adds the use cases on top (duplicate, delete, finish by date).
2. **The default playlist is a campaign of type `DEFAULT`**, in the same aggregate, with no audience rule, no priority, no manual vetoes and a schedule that is always on. KIO-56 makes its existence mandatory.
3. **A published surface for the rules module is defined now:** an immutable, read-only view of a campaign and a query port, in the base package of `campaigns`, so `rules` never touches the module's internals.
4. **A campaign can be edited in any status except `FINISHED`**; changes apply immediately and the invariants are checked on every change. An active or scheduled campaign cannot be edited into an incomplete state.
5. **The domain is organized by building block** (decided on 2026-10-14): the `domain` package is split into `entity`, `event`, `exception`, `repository`, `service` and `valueobject`, with dependency rules between them enforced by tests (R19).
6. **The aggregate records domain events for its status changes but publishes none** (R20). Publishing them, and timestamping them, is KIO-54.
7. **Business behavior is specified with Behavior-Driven Development, using Cucumber** (R21): Gherkin scenarios written in English, run as part of `./mvnw test`, hybrid with unit tests. This becomes a convention of the backend (R17.5).

Source: Trello card KIO-24, the administration document, `docs/kiosko-arquitectura-mvp.md`, `AGENTS.md`, and the specs of KIO-7 (module and layer structure).

## Requirements

### R1 Campaign aggregate and identity

**User story:** As a developer, I want a campaign aggregate with a clear identity and creation rules so that every campaign is valid from the moment it exists.

Acceptance criteria:

- R1.1 The system shall provide a `Campaign` aggregate root in the `domain.entity` package of the `campaigns` module.
- R1.2 Each campaign shall have an identity (`CampaignId`) that never changes, and shall belong to exactly one store (`StoreId`), which shall never change.
- R1.3 Each campaign shall have a type, `REGULAR` or `DEFAULT`, fixed at creation.
- R1.4 When a campaign is created, the system shall set its status to `DRAFT` and shall accept it incomplete (see R12).
- R1.5 If a campaign is created without a store id, without a name or with an invalid type, then the system shall reject the creation (see R13).
- R1.6 Two campaigns shall be equal if and only if they have the same `CampaignId`.

### R2 Name

**User story:** As an administrator, I want each campaign to have a recognizable name so that I can find it in the list.

Acceptance criteria:

- R2.1 The name shall be a text with at least one non-blank character and a maximum length (proposal: 100 characters; confirm in design).
- R2.2 The system shall trim leading and trailing whitespace from the name.
- R2.3 When the name is changed, the system shall apply the same rules as at creation.

### R3 Priority

**User story:** As an administrator, I want to set how important a campaign is so that the rules engine can break ties.

Acceptance criteria:

- R3.1 The priority shall be one of `HIGH`, `MEDIUM` or `LOW` (alta, media, baja), with `MEDIUM` as the default for a `REGULAR` campaign.
- R3.2 While the type is `DEFAULT`, the system shall not allow a priority.
- R3.3 The tie-break behavior that uses the priority belongs to KIO-28 and is out of scope here.

### R4 Audience rule reference

**User story:** As an administrator, I want each campaign to point to one audience rule so that the group it targets is defined in one reusable place.

Acceptance criteria:

- R4.1 A `REGULAR` campaign shall reference at most one audience rule, through an `AudienceRuleId` value object; the audience rule itself is another aggregate (KIO-50) and is not loaded or owned by the campaign.
- R4.2 While the type is `DEFAULT`, the system shall not allow an audience rule.
- R4.3 A `REGULAR` campaign without an audience rule is valid as a draft and is incomplete for scheduling or activation (see R12).
- R4.4 The campaign shall not know the internals of an audience rule.

### R5 Playlist

**User story:** As an administrator, I want the campaign to play an ordered list of media, each with its own duration, so that I control what the screen shows and for how long.

Acceptance criteria:

- R5.1 The playlist shall be an ordered list of items; each item shall have a media reference (`MediaRef`) and a duration in seconds.
- R5.2 The media reference shall be the content hash of the file in the form `sha256:` followed by 64 hexadecimal characters, as in the campaign contract; the media itself is another concern (KIO-31).
- R5.3 The duration of each item shall be a whole number of seconds between a minimum and a maximum (proposal: 1 to 3600; confirm in design).
- R5.4 The system shall allow adding an item (at the end or at a position), removing an item, moving an item to another position and changing the duration of an item, keeping the order consistent and without gaps.
- R5.5 The playlist shall have a maximum number of items (proposal: 50; confirm in design).
- R5.6 The same media may appear more than once in a playlist.
- R5.7 A playlist may be empty while the campaign is a draft; it is incomplete for scheduling or activation (see R12).
- R5.8 The playback loop (items shown in order, repeating) is a behavior of the player and is out of scope.

### R6 Schedule

**User story:** As an administrator, I want to define when a campaign runs, by dates, days of the week and a time slot.

Acceptance criteria:

- R6.1 A schedule shall consist of an optional start date, an optional end date, a non-empty set of days of the week, and an optional time slot (start time and end time).
- R6.2 If both dates are present, then the end date shall not be before the start date.
- R6.3 The days of the week shall default to all seven days.
- R6.4 If a time slot is present, then its start time and end time shall differ; a slot whose start is later than its end crosses midnight and is valid; the absence of a slot means the whole day.
- R6.5 A schedule shall not contain a time zone: dates and times are local to the store, and the time zone is a server setting (KIO-62).
- R6.6 While the type is `DEFAULT`, the schedule shall be always on and shall not be changeable.
- R6.7 Evaluating whether a campaign applies at a given moment belongs to the rules engine (KIO-25) and is out of scope here.

### R7 Scope

**User story:** As an administrator, I want to choose whether a campaign runs on all kiosks or only on some.

Acceptance criteria:

- R7.1 The scope shall be either all kiosks of the store or a non-empty set of kiosk identifiers (`KioskId`).
- R7.2 The default scope shall be all kiosks.
- R7.3 A scope that lists kiosks shall not contain duplicates.
- R7.4 The campaign shall not check that the listed kiosks exist; that belongs to the kiosk configuration (KIO-57) and to the use cases.

### R8 Minimum on-screen time

**User story:** As an administrator, I want a campaign to stay on screen for a minimum time before it can be replaced, so that the screen does not flicker.

Acceptance criteria:

- R8.1 The minimum on-screen time of a `REGULAR` campaign shall be a whole number of seconds between a minimum and a maximum (proposal: 1 to 3600; default 8; confirm in design).
- R8.2 While the type is `DEFAULT`, the system shall not allow a minimum on-screen time.
- R8.3 Applying the minimum time belongs to the rules engine (KIO-28) and is out of scope here.

### R9 Manual vetoes

**User story:** As an administrator, I want to add vetoes by hand to a campaign, in addition to those that apply automatically through content tags.

Acceptance criteria:

- R9.1 A `REGULAR` campaign shall hold a set of manually added vetoes, each referenced through a `VetoId` value object; the veto aggregate is KIO-51.
- R9.2 The set shall not contain duplicates.
- R9.3 While the type is `DEFAULT`, the system shall not allow manual vetoes.
- R9.4 The vetoes that apply automatically (by the content tags of the media) are calculated by KIO-53 and are not stored in the campaign.

### R10 Default campaign

**User story:** As an operator, I want a default playlist that is shown when no other campaign matches, handled like any other campaign by the list and the player.

Acceptance criteria:

- R10.1 A campaign of type `DEFAULT` shall have a name, a playlist, a status and a scope, and shall not have an audience rule, a priority, a minimum on-screen time, manual vetoes or an editable schedule (see R3, R4, R6, R8, R9).
- R10.2 The system shall not allow the type of an existing campaign to change.
- R10.3 The rule that a store must have exactly one `DEFAULT` campaign, and the warning when it is missing, belong to KIO-56 and the repository port (R14.4) and are out of scope here.

### R11 Status and transitions

**User story:** As an administrator, I want a campaign to move through clear statuses so that I know whether it is being prepared, waiting, running, stopped or over.

Acceptance criteria:

- R11.1 The status shall be one of `DRAFT`, `SCHEDULED`, `ACTIVE`, `PAUSED` or `FINISHED` (borrador, programada, activa, pausada, finalizada).
- R11.2 The aggregate shall provide these transitions and no others:
  - `schedule`: from `DRAFT` to `SCHEDULED`; it requires a start date in the schedule;
  - `activate`: from `DRAFT`, `SCHEDULED` or `PAUSED` to `ACTIVE`;
  - `pause`: from `SCHEDULED` or `ACTIVE` to `PAUSED`;
  - `finish`: from `SCHEDULED`, `ACTIVE` or `PAUSED` to `FINISHED`.
- R11.3 If a transition is requested from a status where it is not allowed, then the system shall reject it with a domain error that names the current status and the requested transition (see R13).
- R11.4 `FINISHED` shall be terminal: a finished campaign shall accept no transition and no change.
- R11.5 `schedule` and `activate` shall require the campaign to be complete (see R12.1); `pause` and `finish` shall not.
- R11.6 The aggregate shall not read the clock: moving from `SCHEDULED` to `ACTIVE` when the start date arrives, and to `FINISHED` when the end date passes, are automatic transitions decided by use cases with a clock (KIO-54), which call the transitions above.
- R11.7 Whether a `SCHEDULED` and an `ACTIVE` campaign are both candidates for the rules engine is decided by the engine (KIO-25) and is out of scope here.

### R12 Completeness and editing

**User story:** As an administrator, I want to save drafts that are not finished, but never to run an incomplete campaign.

Acceptance criteria:

- R12.1 A campaign shall be **complete** when it has a name and a non-empty playlist, and, for a `REGULAR` campaign, also an audience rule and a valid schedule.
- R12.2 The system shall allow editing the name, the playlist, the audience rule, the priority, the schedule, the scope, the minimum on-screen time and the manual vetoes while the status is not `FINISHED`.
- R12.3 While the status is `SCHEDULED` or `ACTIVE`, the system shall reject any change that would leave the campaign incomplete, and the campaign shall keep its previous valid state.
- R12.4 While the status is `DRAFT` or `PAUSED`, the system shall accept incomplete states.
- R12.5 Every change shall check all the invariants of R2 to R9 again, so that an invalid state is never reachable through the aggregate's public behavior.
- R12.6 When a change is rejected, the aggregate shall be left unchanged.

### R13 Domain errors

**User story:** As a developer of the API and the UI, I want domain errors that identify the field and the reason so that they can be shown next to the right input.

Acceptance criteria:

- R13.1 Every violation of an invariant shall be reported with a domain error that carries a stable machine-readable code (English, for example `name.blank`) and the name of the affected field.
- R13.2 The domain errors shall not depend on any framework, and their messages shall be in English.
- R13.3 The domain errors shall be enough for the API (KIO-58) to build errors per field without parsing text.
- R13.4 The system shall not use generic exceptions to report business rule violations.

### R14 Repository port

**User story:** As a developer of the persistence layer, I want a repository interface owned by the domain so that the storage technology can be chosen and replaced without changing the domain.

Acceptance criteria:

- R14.1 The system shall define a `CampaignRepository` interface (port) in the `domain.repository` package, with no dependency on Spring, JPA, Jakarta Persistence, JDBC or Jackson.
- R14.2 The port shall at least allow: saving a campaign, finding one by store and id, listing the campaigns of a store, and deleting one by store and id.
- R14.3 Every operation of the port shall take the store id, so that a query can never cross stores.
- R14.4 The port shall allow finding the `DEFAULT` campaign of a store.
- R14.5 Filtering by text, status and audience rule for the list view, and paging, are defined with the API (KIO-58) and are out of scope here.
- R14.6 This feature shall provide no implementation of the port except an in-memory implementation used by tests.

### R15 Published read-only view and query port

**User story:** As the rules module, I want to read the campaigns of a store without depending on how the `campaigns` module is built.

Acceptance criteria:

- R15.1 The base package of the `campaigns` module (its published surface) shall contain an immutable `CampaignView` with the data the rules engine needs: id, store, type, status, priority, schedule, scope, minimum on-screen time, audience rule id, manual veto ids and the playlist.
- R15.2 The view shall be made only of simple values and types of the published surface; it shall not expose the aggregate, its value objects of the `domain` package or any mutating behavior.
- R15.3 The base package shall also contain a query port (`CampaignCatalog`) that returns views: at least the campaigns of a store in a given status and the default campaign of a store.
- R15.4 The implementation of the query port is an adapter (KIO-49 and later) and is out of scope here.
- R15.5 When the module structure is verified, the system shall confirm that the `rules` module can use the published surface and cannot use the `campaigns` internals.

### R16 Architecture and tests

**User story:** As the architect, I want the domain proven by plain unit tests and protected by the architecture rules.

Acceptance criteria:

- R16.1 The `domain` package of `campaigns` shall depend on nothing but the Java standard library (and its own packages), as enforced by the existing ArchUnit rules, and the dependencies between its sub-packages shall follow R19.3.
- R16.2 Every invariant of R2 to R9, every valid and every invalid transition of R11 and the rules of R12 shall have at least one unit test that fails if the rule is removed.
- R16.3 The unit tests shall be named `*Test`, shall not start a Spring context, a database or a network connection, and shall run with `./mvnw test`, together with the Cucumber scenarios of R21.
- R16.4 The tests shall build campaigns through a test data builder so that a test states only what it cares about.
- R16.5 The in-memory repository of R14.6 shall be tested against the contract of the port.
- R16.6 `./mvnw -B -ntp verify` shall pass, including the module verification and the architecture rules, and the CI shall stay green without changing the workflow.

### R17 Documentation

**User story:** As a developer, I want a short description of the campaign domain and a glossary so that the code and the Spanish business documents use the same words.

Acceptance criteria:

- R17.1 `services/core-backend/README.md` shall describe the campaign aggregate, its statuses and transitions (as a diagram or table), its invariants and the published surface.
- R17.2 The README shall include a glossary that maps the Spanish business terms to the English identifiers (campaña, regla de audiencia, veto, medio, playlist, programación, alcance, borrador, programada, activa, pausada, finalizada, prioridad, alta, media, baja).
- R17.3 `AGENTS.md` shall be updated in the same pull request: the project status line and the note that domain code has started (KIO-24).
- R17.4 The Trello card shall be updated at the end with what was delivered and what remains (not part of the code).
- R17.5 `AGENTS.md` shall record the backend convention: the business behavior of every domain is specified as Gherkin scenarios in English under `src/test/resources/features/<module>/`, with steps in a `bdd` test package, run by `./mvnw test`; unit tests cover the technical details.
- R17.6 `services/core-backend/README.md` shall explain how to run all scenarios, a single feature and a single tag, where the feature files and the step definitions live, and how the scenario tags trace to the requirement ids.

### R18 Constraints carried over

**User story:** As the project owner, I want the privacy, language and layering rules to hold.

Acceptance criteria:

- R18.1 The campaign shall hold no personal data of people; it only references rules, vetoes, media and kiosks by identifier.
- R18.2 Identifiers, comments and messages in the code shall be in English; the contract field names (`edad`, `genero`, and so on) shall not appear in the domain, because the mapping to and from the JSON contract belongs to import/export (KIO-30).
- R18.3 This feature shall add to the `pom.xml` only **test-scope** dependencies for Cucumber (MIT) and PicoContainer (BSD) and the JUnit suite, and shall add no runtime dependency, no database migration and no change to `contracts/`.
- R18.4 This feature shall record domain events (R20) but shall not publish them, and shall not read the clock, the environment or the network.

### R19 Domain package structure

**User story:** As a developer, I want the domain organized by building block so that each kind of class has one obvious place and the dependencies between them stay clean.

Acceptance criteria:

- R19.1 The `domain` package of the `campaigns` module shall contain exactly these sub-packages, each with a package description: `entity`, `event`, `exception`, `repository`, `service` and `valueobject`.
- R19.2 The sub-packages shall hold:
  - `entity`: the aggregate root `Campaign`;
  - `event`: the domain events of R20;
  - `exception`: the domain errors of R13;
  - `repository`: the repository port of R14;
  - `service`: stateless domain services that hold rules which do not belong to a single value object (the status transition policy and the invariants checker);
  - `valueobject`: identifiers, value objects, the enums (`CampaignType`, `CampaignStatus`, `Priority`) and the immutable `CampaignSnapshot`.
- R19.3 The dependencies between the sub-packages shall be:
  - `exception` depends on nothing in the domain;
  - `valueobject` depends only on `exception`;
  - `event` depends only on `valueobject`;
  - `service` depends only on `valueobject` and `exception`;
  - `entity` depends on `valueobject`, `event`, `exception` and `service`;
  - `repository` depends on `entity` and `valueobject`;
  - nothing in the domain depends on `repository` except classes outside the domain.
- R19.4 If a class breaks these dependencies, then the build shall fail with a message that names the class (architecture test).
- R19.5 The rules of R19.3 shall be generic, so they also apply to the `domain` package of every other module.

### R20 Domain events (recorded, not published)

**User story:** As the designer of the other modules, I want the campaign to tell what happened to it so that reports and the player can react later, without the domain knowing who listens.

Acceptance criteria:

- R20.1 The `event` package shall contain a sealed `CampaignEvent` with these events: `CampaignCreated`, `CampaignScheduled`, `CampaignActivated`, `CampaignPaused` and `CampaignFinished`.
- R20.2 Each event shall be immutable and carry the campaign id and the store id; `CampaignCreated` shall also carry the type.
- R20.3 Events shall not carry a time stamp, because the domain does not read the clock; whoever publishes them adds the time (KIO-54).
- R20.4 When a campaign is created, the aggregate shall record `CampaignCreated`; when a status transition succeeds, it shall record the matching event; when a change or a transition is rejected, it shall record nothing.
- R20.5 The aggregate shall hand over the recorded events in order with `pullEvents()` and shall forget them once handed over.
- R20.6 A campaign rebuilt with `restore` shall have no pending events.
- R20.7 Edits that are not status transitions shall not record events in this feature.
- R20.8 No class shall publish, dispatch or listen to these events in this feature.

### R21 Behavior-driven specification with Cucumber

**User story:** As a developer and as the business owner, I want the campaign behavior written as readable scenarios that also run as tests, so that the rules are documented and verified in the same place.

Acceptance criteria:

- R21.1 The business behavior of the campaign (creation as a draft, status lifecycle, completeness, editing in each status, the default campaign, playlist editing, schedule and scope, and the recorded events) shall be specified as Gherkin scenarios in **English**, in `.feature` files under `src/test/resources/features/campaigns/`.
- R21.2 The scenarios shall use business words (campaign, draft, scheduled, active, paused, finished, playlist, audience rule) and shall not mention Java classes, constructors or exceptions by technical name.
- R21.3 The step definitions shall be Java classes in a `bdd` test package, shall share state per scenario without static fields, and shall use the test data builder of R16.4.
- R21.4 The scenarios shall run through the JUnit Platform with `./mvnw test` (a suite class named `*Test`), without a Spring context, a database or a network connection, and a failing scenario shall fail the build.
- R21.5 Each scenario shall carry a tag with the requirement ids it verifies (for example `@R11`), so that a tag can be selected on the command line and the coverage can be read from the files.
- R21.6 Every acceptance criterion of R3 to R12 and R20 shall be verified by at least one scenario or, when it is a technical detail of a value object or of the repository contract, by a unit test; the design shall contain the table that shows which.
- R21.7 Scenario outlines shall be used for the status transition matrix, so that every pair of status and transition is covered by one table.
- R21.8 The reports of a run shall be written under `target/` and not committed.
- R21.9 The scenarios and the unit tests shall not duplicate each other: a rule is verified at the level where it is meaningful to the business (scenario) or technical (unit test), as listed in the design.

## Out of scope

- Persistence of campaigns, migrations and the repository adapter (KIO-49).
- The audience rule aggregate (KIO-50), the veto aggregate (KIO-51), media and content tags (KIO-31, KIO-52, KIO-53).
- Use cases with a clock: scheduling by date, finishing by date, duplicating and deleting (KIO-54).
- Validation warnings before activation (KIO-55) and the mandatory default playlist (KIO-56).
- Evaluating the schedule, scope, vetoes, audience, priority and minimum time (KIO-25 to KIO-28).
- REST API, filtering and paging (KIO-58); import and export and the JSON contract mapping (KIO-30).
- Timestamps, audit data and optimistic locking (persistence concerns).
- Publishing, dispatching or timestamping domain events, and domain events for edits that are not status transitions (KIO-54).

## Open questions (to settle in design.md)

1. How the aggregate keeps its state now that `CampaignSnapshot` is a value object (proposal: the snapshot is the internal state) and how the services read it.
2. How domain errors are modeled and the catalog of codes (a single exception carrying a list of violations).
3. The concrete bounds: name length, playlist size, item duration, minimum on-screen time and its default.
4. How identifiers are generated and whether `StoreId` and `KioskId` are free text or constrained.
5. Whether the aggregate is mutable with behavior methods or immutable.
6. Whether `duplicate` is behavior of the aggregate or entirely a use case of KIO-54.
7. The exact meaning of `SCHEDULED` versus `ACTIVE` and who flips them (this spec proposes: `schedule` requires a start date and KIO-54 flips by date).
8. How the published view is built without exposing domain types.
9. The Cucumber setup: versions and the dependency management, how the suite finds the features and the glue, how state is shared between step classes, the reports, and how a single tag or feature is run.
10. Which behaviors are scenarios and which are unit tests (hybrid), and the traceability table.
11. How the dependency rules between the domain sub-packages are written as architecture tests and proven with fixtures.
