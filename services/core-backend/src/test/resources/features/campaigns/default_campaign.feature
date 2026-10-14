@R10
Feature: The default campaign
  The default campaign plays when no other campaign matches, so it has no targeting settings.

  Background:
    Given a store "tienda-001"

  @R10.1 @R3.2 @R4.2 @R6.6 @R8.2 @R9.3
  Scenario Outline: The default campaign rejects every regular-only setting
    Given a default campaign with 1 playlist item
    When the administrator tries to give the default campaign <setting>
    Then the change is rejected because <reason>
    And the campaign is unchanged

    Examples:
      | setting            | reason                                               |
      | a priority         | the default campaign cannot have a priority          |
      | an audience rule   | the default campaign cannot have an audience rule    |
      | a minimum time     | the default campaign cannot have a minimum time      |
      | a manual veto      | the default campaign cannot have manual vetoes       |
      | a custom schedule  | the schedule of the default campaign cannot change   |

  @R10.1 @R11.5
  Scenario: The default campaign can be activated once it has a playlist
    Given a default campaign with 1 playlist item
    When the administrator tries to activate the campaign
    Then the campaign status is "active"

  @R12.1
  Scenario: The default campaign cannot be activated without a playlist
    Given a default campaign with 0 playlist items
    When the administrator tries to activate the campaign
    Then the change is rejected because the playlist is empty
    And the campaign status is "draft"
