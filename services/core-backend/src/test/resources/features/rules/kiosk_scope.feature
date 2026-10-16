@KIO-25 @R6
Feature: A campaign runs on all the kiosks or only on the ones it was given

  Background:
    Given a store "tienda-001" and a kiosk "kiosk-a"
    And the server time zone is "Europe/Madrid"

  @R6.1
  Scenario: A campaign for all kiosks includes every kiosk
    Given a campaign in status "active"
    Then the campaign is a candidate for the kiosk

  @R6.2
  Scenario: A listed kiosk is included
    Given a campaign in status "active"
    And it runs only on the kiosks "kiosk-a, kiosk-b"
    Then the campaign is a candidate for the kiosk

  @R6.2
  Scenario: A kiosk that is not listed is excluded
    Given a campaign in status "active"
    And it runs only on the kiosks "kiosk-b, kiosk-c"
    Then the campaign is not a candidate for the kiosk
    And the reason is "the kiosk is not included"

  @R6.3
  Scenario: The kiosk id is compared exactly
    Given a campaign in status "active"
    And it runs only on the kiosks "Kiosk-A"
    Then the reason is "the kiosk is not included"
