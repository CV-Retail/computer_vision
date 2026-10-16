@KIO-25 @R3
Feature: A campaign runs only between its start and end dates
  Both dates are included, from the first moment of the start date to the last moment of the end date.

  Background:
    Given a store "tienda-001" and a kiosk "kiosk-a"
    And the server time zone is "Europe/Madrid"

  @R3.1 @R3.4
  Scenario Outline: The start date is included from its first moment
    Given a campaign in status "active"
    And it starts on "2030-01-14"
    When the server time is "<moment>"
    Then the result is "<result>"

    Examples:
      | moment           | result                          |
      | 2030-01-13 23:59 | the start date has not arrived  |
      | 2030-01-14 00:00 | a candidate                     |
      | 2030-02-01 12:00 | a candidate                     |

  @R3.2
  Scenario Outline: The end date is included until its last moment
    Given a campaign in status "active"
    And it ends on "2030-01-14"
    When the server time is "<moment>"
    Then the result is "<result>"

    Examples:
      | moment           | result                   |
      | 2029-12-01 12:00 | a candidate              |
      | 2030-01-14 23:59 | a candidate              |
      | 2030-01-15 00:00 | the end date has passed  |

  @R3.3
  Scenario: A campaign without dates has no limit
    Given a campaign in status "active"
    When the server time is "2045-06-15 12:00"
    Then the campaign is a candidate for the kiosk

  @R3.1 @R3.2
  Scenario: A campaign runs inside its dates
    Given a campaign in status "active"
    And it starts on "2030-01-10"
    And it ends on "2030-01-20"
    When the server time is "2030-01-15 12:00"
    Then the campaign is a candidate for the kiosk
