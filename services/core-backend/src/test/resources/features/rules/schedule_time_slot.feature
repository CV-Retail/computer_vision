@KIO-25 @R5
Feature: A campaign runs only inside its time slot
  The slot starts at its first minute and ends before its last one. A slot may cross midnight.

  Background:
    Given a store "tienda-001" and a kiosk "kiosk-a"
    And the server time zone is "Europe/Madrid"

  @R5.1
  Scenario Outline: A campaign without a slot runs all day
    Given a campaign in status "active"
    When the server time is "<moment>"
    Then the campaign is a candidate for the kiosk

    Examples:
      | moment           |
      | 2030-01-14 00:00 |
      | 2030-01-14 23:59 |

  @R5.2 @R5.3
  Scenario Outline: Moments around a slot from 09:00 to 18:00
    Given a campaign in status "active"
    And it runs between "09:00" and "18:00"
    When the server time is "<moment>"
    Then the result is "<result>"

    Examples:
      | moment           | result                       |
      | 2030-01-14 08:59 | it is outside the time slot  |
      | 2030-01-14 09:00 | a candidate                  |
      | 2030-01-14 17:59 | a candidate                  |
      | 2030-01-14 18:00 | it is outside the time slot  |

  @R5.4
  Scenario Outline: A Monday slot from 22:00 to 02:00 belongs to the day it started
    Given a campaign in status "active"
    And it runs only on "monday"
    And it starts on "2030-01-14"
    And it ends on "2030-01-14"
    And it runs between "22:00" and "02:00"
    When the server time is "<moment>"
    Then the result is "<result>"

    Examples:
      | moment           | result                          |
      | 2030-01-14 21:59 | it is outside the time slot     |
      | 2030-01-14 22:00 | a candidate                     |
      | 2030-01-14 23:59 | a candidate                     |
      | 2030-01-15 00:00 | a candidate                     |
      | 2030-01-15 01:59 | a candidate                     |
      | 2030-01-15 02:00 | the end date has passed         |
      | 2030-01-14 01:00 | the start date has not arrived  |

  @R5.4
  Scenario: The part after midnight uses the day the slot started
    Given a campaign in status "active"
    And it runs only on "tuesday"
    And it runs between "22:00" and "02:00"
    When the server time is "2030-01-15 01:00"
    Then the reason is "the day is not included"

  @R5.4
  Scenario: A slot that crosses midnight is closed during the day
    Given a campaign in status "active"
    And it runs between "22:00" and "02:00"
    When the server time is "2030-01-14 12:00"
    Then the reason is "it is outside the time slot"
