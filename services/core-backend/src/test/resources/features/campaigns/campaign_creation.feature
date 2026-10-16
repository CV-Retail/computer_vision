@KIO-24
@R1
Feature: Creating a campaign
  An administrator starts every campaign as a draft that may still be incomplete.

  Background:
    Given a store "tienda-001"

  @R1.3 @R3.1 @R7.2 @R8.1
  Scenario: A new regular campaign is a draft with sensible defaults
    When the administrator creates a regular campaign named "Summer sale"
    Then the campaign status is "draft"
    And it is a "regular" campaign
    And the campaign belongs to the store "tienda-001"
    And its name is "Summer sale"
    And its priority is "medium"
    And its minimum time on screen is 8 seconds
    And it runs on all kiosks
    And it runs every day without dates
    And its playlist is empty
    And it has no audience rule
    And it has no manual vetoes

  @R1.3 @R10.1
  Scenario: A new default campaign is a draft without regular-only settings
    When the administrator creates the default campaign named "Default playlist"
    Then the campaign status is "draft"
    And it is a "default" campaign
    And it has no priority
    And it has no minimum time on screen
    And it has no audience rule
    And it has no manual vetoes
    And it runs on all kiosks

  @R2
  Scenario: A campaign needs a name
    When the administrator creates a regular campaign named "   "
    Then the change is rejected because the name is blank
