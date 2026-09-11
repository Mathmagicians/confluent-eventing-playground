Feature: Alice's purse knows what is left
  A purse holds coins for its owner and reads the stream of transactions. When the owner sold, the price goes in;
  when the owner bought, it comes out; the others' trades pass by. The purse knows what is left, and when it is
  empty and the owner shops on, it says what she owes.

  Background:
    Given I have the API keys to the cluster

  Scenario Outline: The purse follows its owner's trades and nobody else's
    Given <Owner>'s purse holds <Coins> coins
    And the tea party has settled orders from <Owner>, offers by <Owner>, and trades between others
    When the purse reads the stream of transactions
    Then it put in the price of every transaction where <Owner> is the seller
    And took out the price of every transaction where <Owner> is the customer
    And nothing for the others
    And it reports what is left

    Examples:
      | Owner        | Coins |
      | Alice        | 1000  |
      | White Rabbit | 500   |
      | Cheshire Cat | 250   |

  Scenario Outline: An empty purse says what its owner owes
    Given <Owner>'s purse holds <Coins> coins
    And the tea party has settled orders from <Owner> worth more than that
    When the purse reads the stream of transactions
    Then the purse is empty
    And it reports what <Owner> owes
    And it says so where a human will look

    Examples:
      | Owner           | Coins |
      | Alice           | 1     |
      | Mad Hatter      | 10    |
      | Queen of Hearts | 0     |
