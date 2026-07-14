Feature: Complete shopping journey

  As a shopper
  I want to create and manage shopping lists
  So that I can organise my shopping and stay within budget

  Scenario: Register, login and complete a shopping trip

    Given a new shopper registers with:
      | name     | Scott Varns       |
      | email    | scott@example.com |
      | password | Password123!      |
    Then the registration should succeed

    When the shopper logs in with:
      | email    | scott@example.com |
      | password | Password123!      |
    Then the login should succeed
    And an access token should be returned

    When the shopper creates a shopping list with:
      | listName      | Weekly Shop |
      | spendingLimit | 20.00       |
      | date          | 2026-07-18  |
    Then the shopping list should be created successfully

    When the shopper retrieves the shopping list
    Then the shopping list should be named "Weekly Shop"
    And the shopping list should contain 0 items
    And the shopping list total should be 0.00
    And the shopping list should not be over budget

    When the shopper adds the following items:
      | itemName | quantity | unitPrice |
      | Milk     | 1        | 1.50      |
      | Bread    | 1        | 1.25      |
      | Chicken  | 1        | 6.00      |
      | Rice     | 2        | 2.50      |
      | Coffee   | 1        | 7.50      |

    And the shopper retrieves the shopping list
    Then the shopping list should contain 5 items
    And the shopping list total should be 21.25
    And the shopping list should be over budget
    And the remaining budget should be -1.25
    And the shopping list should contain:
      | itemName | purchased |
      | Milk     | false     |
      | Bread    | false     |
      | Chicken  | false     |
      | Rice     | false     |
      | Coffee   | false     |

    When the shopper removes "Coffee"

    And the shopper retrieves the shopping list
    Then the shopping list should contain 4 items
    And the shopping list total should be 13.75
    And the shopping list should not be over budget
    And the remaining budget should be 6.25

    When the shopper reorders the shopping list to:
      | position | itemName |
      | 1        | Chicken  |
      | 2        | Rice     |
      | 3        | Bread    |
      | 4        | Milk     |

    And the shopper retrieves the shopping list
    Then the shopping list items should be returned in this order:
      | position | itemName |
      | 1        | Chicken  |
      | 2        | Rice     |
      | 3        | Bread    |
      | 4        | Milk     |

    When the shopper marks "Chicken" as purchased
    And the shopper marks "Rice" as purchased

    And the shopper retrieves the shopping list
    Then "Chicken" should be marked as purchased
    And "Rice" should be marked as purchased
    And "Bread" should not be marked as purchased
    And "Milk" should not be marked as purchased
    And the shopping list should contain 2 purchased items
    And the shopping list should contain 2 unpurchased items
