@R12
Feature: Editing a campaign
  Every field can be edited while the campaign is not finished.

  Background:
    Given a store "tienda-001"

  @R3.1
  Scenario: The priority of a regular campaign can be changed
    Given a regular draft campaign
    When the administrator sets the priority to "high"
    Then the change is accepted
    And its priority is "high"

  @R4.1 @R4.3
  Scenario: An audience rule can be assigned and removed on a draft
    Given a regular draft campaign
    When the administrator assigns an audience rule
    Then the campaign has an audience rule
    When the administrator removes the audience rule
    Then it has no audience rule

  @R8.1
  Scenario: The minimum time on screen can be changed
    Given a regular draft campaign
    When the administrator sets the minimum time on screen to 20 seconds
    Then its minimum time on screen is 20 seconds

  @R8.1
  Scenario: A minimum time on screen out of range is rejected
    Given a regular draft campaign
    When the administrator sets the minimum time on screen to 0 seconds
    Then the change is rejected because the minimum time is out of range
    And the campaign is unchanged

  @R9.1
  Scenario: Manual vetoes can be added and removed
    Given a regular draft campaign
    When the administrator adds a veto
    Then the campaign has 1 manual veto
    When the administrator removes the veto
    Then the campaign has 0 manual vetoes

  @R9.2
  Scenario: The same veto cannot be added twice
    Given a regular draft campaign
    And a veto is on the campaign
    When the administrator adds the same veto again
    Then the change is rejected because the veto is already on the campaign
    And the campaign has 1 manual veto

  @R9.2
  Scenario: A veto that is not on the campaign cannot be removed
    Given a regular draft campaign
    When the administrator removes a veto that is not on the campaign
    Then the change is rejected because the veto is not on the campaign

  @R12.2
  Scenario: An active campaign can still be renamed
    Given an active regular campaign with one playlist item
    When the administrator renames the campaign to "Winter sale"
    Then the change is accepted
    And its name is "Winter sale"
    And the campaign status is "active"

  @R11.4 @R12.2
  Scenario Outline: A finished campaign accepts no change
    Given a complete regular campaign in status "finished"
    When <change>
    Then the change is rejected because the campaign is finished
    And the campaign is unchanged

    Examples:
      | change                                                      |
      | the administrator renames the campaign to "Another name"    |
      | the administrator sets the priority to "low"                |
      | the administrator adds the item 7 to the playlist           |
      | the administrator restricts the campaign to the kiosks "k1" |
