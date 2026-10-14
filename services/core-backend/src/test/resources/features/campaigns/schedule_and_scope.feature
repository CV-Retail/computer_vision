@R6 @R7
Feature: Schedule and scope
  A campaign runs on certain dates, days and hours, and on all kiosks or only on some.

  Background:
    Given a store "tienda-001"
    And a regular draft campaign

  @R6.1 @R6.3
  Scenario: A new campaign runs every day
    Then the campaign runs every day

  @R6.1
  Scenario: A campaign can run between two dates
    When the administrator schedules the campaign from "2030-01-10" to "2030-01-20"
    Then the campaign runs from "2030-01-10" to "2030-01-20"

  @R6.2
  Scenario: The end date cannot be before the start date
    When the administrator schedules the campaign from "2030-01-20" to "2030-01-10"
    Then the change is rejected because the end date is before the start date
    And the campaign is unchanged

  @R6.1
  Scenario: A campaign can run on some days only
    When the administrator runs the campaign only on "monday, friday"
    Then the campaign runs only on "monday, friday"

  @R6.1
  Scenario: At least one day is required
    When the administrator runs the campaign only on ""
    Then the change is rejected because no days are selected

  @R6.4
  Scenario: A time slot can cross midnight
    When the administrator runs the campaign between "22:00" and "02:00"
    Then the campaign runs between "22:00" and "02:00"

  @R6.4
  Scenario: A time slot cannot start and end at the same time
    When the administrator runs the campaign between "12:00" and "12:00"
    Then the change is rejected because the time slot is empty

  @R7.1
  Scenario: A campaign can be limited to some kiosks
    When the administrator restricts the campaign to the kiosks "kiosk-a, kiosk-b"
    Then the campaign runs only on the kiosks "kiosk-a, kiosk-b"

  @R7.1
  Scenario: A campaign can go back to all kiosks
    Given the administrator restricts the campaign to the kiosks "kiosk-a"
    When the administrator makes the campaign run on all kiosks
    Then the campaign runs on all kiosks

  @R7.1
  Scenario: Limiting to no kiosk is rejected
    When the administrator restricts the campaign to the kiosks ""
    Then the change is rejected because no kiosks are selected

  @R7.3
  Scenario: A kiosk cannot be listed twice
    When the administrator restricts the campaign to the kiosks "kiosk-a, kiosk-a"
    Then the change is rejected because a kiosk is listed twice
    And the campaign is unchanged
