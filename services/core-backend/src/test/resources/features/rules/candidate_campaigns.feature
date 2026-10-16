@KIO-25 @R1
Feature: Candidate campaigns for a kiosk
  The first step of the engine lists the campaigns of the store that may run on the kiosk right now.

  Background:
    Given a store "tienda-001" and a kiosk "kiosk-a"
    And the server time zone is "Europe/Madrid"
    And the server time is "2030-01-14 12:00"

  @R1.4 @R2.3
  Scenario: Active campaigns come first, then scheduled ones, each group in the catalog order
    Given a campaign "Winter" in status "scheduled"
    And a campaign "Summer" in status "active"
    And a campaign "Autumn" in status "scheduled"
    And a campaign "Spring" in status "active"
    Then the candidates are "Summer, Spring, Winter, Autumn"

  @R1.3
  Scenario: Only the campaigns that pass every check are candidates
    Given a campaign "Open" in status "active"
    And a campaign "Paused" in status "paused"
    And a campaign "Other kiosk" in status "active"
    And it runs only on the kiosks "kiosk-b"
    And a campaign "Ended" in status "active"
    And it ends on "2030-01-13"
    Then the candidates are "Open"

  @R1.5
  Scenario: A campaign of another store is ignored
    Given a campaign "Summer" in status "active"
    And it belongs to the store "tienda-002"
    Then there are no candidates

  @R1.6
  Scenario: A store without campaigns has no candidates
    Then there are no candidates

  @R7.3 @R3.5
  Scenario Outline: The same instant is a different day in a different zone
    Given the server instant is "2030-01-14T23:30:00Z"
    And the server time zone is "<zone>"
    And a campaign in status "active"
    And it runs only on "monday"
    Then the result is "<result>"

    Examples:
      | zone                | result                  |
      | Europe/Madrid       | the day is not included |
      | America/Los_Angeles | a candidate             |

  @R7.3 @R5.5
  Scenario Outline: The time slot follows the local time of the server
    Given the server instant is "2030-01-14T08:30:00Z"
    And the server time zone is "<zone>"
    And a campaign in status "active"
    And it runs between "09:00" and "18:00"
    Then the result is "<result>"

    Examples:
      | zone                | result                      |
      | Europe/Madrid       | a candidate                 |
      | America/Los_Angeles | it is outside the time slot |

  @R7.4
  Scenario Outline: The day the clocks move forward in Madrid
    Given the server instant is "<instant>"
    And a campaign in status "active"
    And it runs between "02:00" and "04:00"
    Then the result is "<result>"

    Examples:
      | instant              | result                      |
      | 2030-03-31T00:59:59Z | it is outside the time slot |
      | 2030-03-31T01:00:00Z | a candidate                 |

  @R8.2
  Scenario: When several reasons hold, the first one is shown
    Given a campaign in status "active"
    And it starts on "2030-02-01"
    And it runs only on "sunday"
    And it runs only on the kiosks "kiosk-b"
    Then the reason is "the start date has not arrived"
