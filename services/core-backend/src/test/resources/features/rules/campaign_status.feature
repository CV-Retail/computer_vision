@KIO-25 @R2
Feature: Only released campaigns can be candidates
  Drafts, paused and finished campaigns are ignored; scheduled and active ones are judged by their calendar.

  Background:
    Given a store "tienda-001" and a kiosk "kiosk-a"
    And the server time zone is "Europe/Madrid"
    And the server time is "2030-01-14 12:00"

  @R2.1 @R2.2 @R2.3
  Scenario Outline: The status decides whether a campaign can be a candidate
    Given a campaign in status "<status>"
    Then the campaign is <outcome> for the kiosk

    Examples:
      | status    | outcome         |
      | draft     | not a candidate |
      | scheduled | a candidate     |
      | active    | a candidate     |
      | paused    | not a candidate |
      | finished  | not a candidate |

  @R2.1 @R8.1
  Scenario: A paused campaign is rejected because it is not released
    Given a campaign in status "paused"
    Then the campaign is not a candidate for the kiosk
    And the reason is "the campaign is not released"
