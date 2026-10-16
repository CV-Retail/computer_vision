@KIO-24
@R12
Feature: A running campaign must be complete
  Drafts and paused campaigns may be incomplete, but a scheduled or active campaign never is.

  Background:
    Given a store "tienda-001"

  @R12.4 @R4.3 @R5.7
  Scenario: A draft can be saved without a playlist or an audience rule
    Given a regular campaign with no playlist and no audience rule
    Then the campaign status is "draft"
    And its playlist is empty
    And it has no audience rule

  @R12.1 @R11.5
  Scenario: An incomplete campaign cannot be scheduled
    Given a regular campaign with no playlist and no audience rule
    When the administrator tries to schedule the campaign
    Then the change is rejected because the playlist is empty
    And the change is rejected because the audience rule is missing
    And the change is rejected because the start date is missing
    And the campaign is unchanged

  @R12.1 @R11.5
  Scenario: An incomplete campaign cannot be activated
    Given a regular campaign with no playlist and no audience rule
    When the administrator tries to activate the campaign
    Then the change is rejected because the playlist is empty
    And the change is rejected because the audience rule is missing
    And the campaign status is "draft"

  @R12.3 @R12.6
  Scenario: An active campaign cannot lose its last playlist item
    Given an active regular campaign with one playlist item
    When the administrator removes the playlist item
    Then the change is rejected because the playlist is empty
    And the campaign is unchanged

  @R12.3
  Scenario: An active campaign cannot lose its audience rule
    Given an active regular campaign with one playlist item
    When the administrator removes the audience rule
    Then the change is rejected because the audience rule is missing
    And the campaign is unchanged

  @R12.3
  Scenario: A scheduled campaign cannot lose its start date
    Given a complete regular campaign in status "scheduled"
    When the administrator schedules the campaign from "2030-03-01" to "2030-02-01"
    Then the change is rejected because the end date is before the start date
    And the campaign is unchanged

  @R12.4
  Scenario: A paused campaign may become incomplete but cannot be resumed until it is complete again
    Given a complete regular campaign in status "paused"
    When the administrator removes the playlist item
    Then the change is accepted
    When the administrator resumes the campaign
    Then the change is rejected because the playlist is empty
    And the campaign status is "paused"
