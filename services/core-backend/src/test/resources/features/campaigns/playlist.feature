@KIO-24
@R5
Feature: Playlist
  The playlist is an ordered list of media, each with its own duration.

  Background:
    Given a store "tienda-001"

  @R5.4
  Scenario: Items are added at the end
    Given a draft campaign with a playlist of 2 items
    When the administrator adds the item 3 to the playlist
    Then the playlist order is "1, 2, 3"

  @R5.4
  Scenario: An item can be inserted at a position
    Given a draft campaign with a playlist of 2 items
    When the administrator inserts the item 9 at position 1
    Then the playlist order is "1, 9, 2"

  @R5.4
  Scenario: An item can be moved to another position
    Given a draft campaign with a playlist of 3 items
    When the administrator moves the item at position 0 to position 2
    Then the playlist order is "2, 3, 1"

  @R5.4
  Scenario: Removing an item leaves no gap
    Given a draft campaign with a playlist of 3 items
    When the administrator removes the item at position 1
    Then the playlist order is "1, 3"

  @R5.3 @R5.4
  Scenario: The duration of one item can be changed
    Given a draft campaign with a playlist of 2 items
    When the administrator changes the duration of the item at position 1 to 30 seconds
    Then the item at position 1 lasts 30 seconds
    And the item at position 0 lasts 10 seconds

  @R5.3
  Scenario Outline: A duration out of range is rejected
    Given a draft campaign with a playlist of 2 items
    When the administrator changes the duration of the item at position 0 to <seconds> seconds
    Then the change is rejected because the duration is out of range
    And the campaign is unchanged

    Examples:
      | seconds |
      | 0       |
      | 3601    |

  @R5.6
  Scenario: The same media can appear twice
    Given a draft campaign with a playlist of 1 items
    When the administrator adds the item 1 to the playlist
    Then the playlist order is "1, 1"

  @R5.5
  Scenario: A playlist has at most 50 items
    Given a draft campaign with a playlist of 50 items
    When the administrator adds the item 51 to the playlist
    Then the change is rejected because the playlist is full
    And the playlist has 50 items

  @R5.4
  Scenario: A position that does not exist is rejected
    Given a draft campaign with a playlist of 2 items
    When the administrator removes the item at position 2
    Then the change is rejected because the position does not exist
    And the campaign is unchanged
