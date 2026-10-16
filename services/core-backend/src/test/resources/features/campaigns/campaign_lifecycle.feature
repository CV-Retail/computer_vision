@KIO-24
@R11
Feature: Campaign life cycle
  A campaign moves from draft to scheduled or active, can be paused and resumed, and ends finished.

  Background:
    Given a store "tienda-001"

  @R11.2 @R11.3 @R11.4 @R11.5
  Scenario Outline: Which transition is allowed from which status
    Given a complete regular campaign in status "<from>"
    When the administrator tries to <transition> the campaign
    Then the outcome is "<outcome>"

    Examples: A draft
      | from  | transition | outcome                           |
      | draft | schedule   | becomes scheduled                 |
      | draft | activate   | becomes active                    |
      | draft | pause      | is rejected: transition not allowed |
      | draft | finish     | is rejected: transition not allowed |

    Examples: A scheduled campaign
      | from      | transition | outcome                             |
      | scheduled | schedule   | is rejected: transition not allowed |
      | scheduled | activate   | becomes active                      |
      | scheduled | pause      | becomes paused                      |
      | scheduled | finish     | becomes finished                    |

    Examples: An active campaign
      | from   | transition | outcome                             |
      | active | schedule   | is rejected: transition not allowed |
      | active | activate   | is rejected: transition not allowed |
      | active | pause      | becomes paused                      |
      | active | finish     | becomes finished                    |

    Examples: A paused campaign
      | from   | transition | outcome                             |
      | paused | schedule   | is rejected: transition not allowed |
      | paused | activate   | becomes active                      |
      | paused | pause      | is rejected: transition not allowed |
      | paused | finish     | becomes finished                    |

    Examples: A finished campaign is terminal
      | from     | transition | outcome                                |
      | finished | schedule   | is rejected: the campaign is finished  |
      | finished | activate   | is rejected: the campaign is finished  |
      | finished | pause      | is rejected: the campaign is finished  |
      | finished | finish     | is rejected: the campaign is finished  |

  @R11.2
  Scenario: A paused campaign can be resumed
    Given a complete regular campaign in status "active"
    When the administrator tries to pause the campaign
    And the administrator resumes the campaign
    Then the campaign status is "active"

  @R11.3
  Scenario: A rejected transition leaves the campaign as it was
    Given a complete regular campaign in status "draft"
    When the administrator tries to pause the campaign
    Then the change is rejected because the transition is not allowed
    And the campaign is unchanged
