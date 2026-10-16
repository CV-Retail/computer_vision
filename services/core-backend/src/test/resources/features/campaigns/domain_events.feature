@KIO-24
@R20
Feature: Events recorded by a campaign
  A campaign tells what happened to it. Nobody publishes these events yet.

  Background:
    Given a store "tienda-001"

  @R20.4
  Scenario: Creating a campaign records one event
    When the administrator creates a regular campaign named "Summer sale"
    And the recorded events are handed over
    Then the recorded events are "created"

  @R20.4 @R20.5
  Scenario: Each status change records its event in order
    Given a complete regular campaign in status "draft"
    When the administrator tries to schedule the campaign
    And the administrator tries to activate the campaign
    And the administrator tries to pause the campaign
    And the administrator resumes the campaign
    And the administrator tries to finish the campaign
    And the recorded events are handed over
    Then the recorded events are "scheduled, activated, paused, activated, finished"

  @R20.4
  Scenario: A rejected transition records nothing
    Given a complete regular campaign in status "draft"
    When the administrator tries to pause the campaign
    Then no events are recorded

  @R20.5
  Scenario: Events are handed over only once
    Given a complete regular campaign in status "draft"
    When the administrator tries to activate the campaign
    And the recorded events are handed over
    Then no events are recorded

  @R20.7
  Scenario: Editing a campaign records no event
    Given a regular draft campaign
    When the administrator renames the campaign to "Winter sale"
    Then no events are recorded

  @R20.6
  Scenario: A campaign rebuilt from its stored state has no pending events
    Given a complete regular campaign in status "active"
    When the campaign is rebuilt from its stored state
    Then no events are recorded
