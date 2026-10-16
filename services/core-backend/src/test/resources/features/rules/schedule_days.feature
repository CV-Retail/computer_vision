@KIO-25 @R4
Feature: A campaign runs only on the days of the week it was given

  Background:
    Given a store "tienda-001" and a kiosk "kiosk-a"
    And the server time zone is "Europe/Madrid"

  @R4.2
  Scenario: A campaign with all the days has no restriction
    Given a campaign in status "active"
    When the server time is "2030-01-19 12:00"
    Then the campaign is a candidate for the kiosk

  @R4.1
  Scenario Outline: A campaign that runs on Monday and Friday
    Given a campaign in status "active"
    And it runs only on "monday, friday"
    When the server time is "<moment>"
    Then the result is "<result>"

    Examples:
      | moment           | result                  |
      | 2030-01-14 12:00 | a candidate             |
      | 2030-01-15 12:00 | the day is not included |
      | 2030-01-18 12:00 | a candidate             |
      | 2030-01-19 12:00 | the day is not included |
      | 2030-01-20 12:00 | the day is not included |

  @R4.1 @R4.3
  Scenario: The day changes at midnight
    Given a campaign in status "active"
    And it runs only on "monday"
    When the server time is "2030-01-14 23:59"
    Then the campaign is a candidate for the kiosk
    When the server time is "2030-01-15 00:00"
    Then the reason is "the day is not included"
