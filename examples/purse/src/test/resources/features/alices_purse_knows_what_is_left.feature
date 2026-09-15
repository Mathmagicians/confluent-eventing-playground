Feature: Alice's purse knows what is left
  A purse holds coins for its owner and reads the stream of transactions. When the owner sold, the price goes in;
  when the owner bought, it comes out; the others' trades pass by. The purse knows what is left, and when it is
  empty and the owner shops on, it says what she owes.

  Background:
    Given I have the API keys to the cluster

  Scenario Outline: The purse follows its owner's trades and nobody else's
    Given <Owner>'s purse holds <Coins> coins
    And Mad Hatter paid <Owner> 20.25 coins for a thing
    And <Owner> paid Mad Hatter 10.5 coins for a thing
    And Mad Hatter paid Dormouse 99 coins for a thing
    When the purse reads the stream of transactions
    Then <Owner> has <Left> coins left
    And the purse said nothing about Mad Hatter or Dormouse

    Examples:
      | Owner        | Coins | Left    |
      | Alice        | 1000  | 1009.75 |
      | White Rabbit | 500   | 509.75  |
      | Cheshire Cat | 250   | 259.75  |

  Scenario Outline: An empty purse says what its owner owes
    Given <Owner>'s purse holds <Coins> coins
    And <Owner> paid Cheshire Cat <Price> coins for a thing
    When the purse reads the stream of transactions
    Then <Owner> owes <Owed> coins
    And the purse makes a warning

    Examples:
      | Owner           | Coins | Price | Owed |
      | Alice           | 1     | 2     | 1    |
      | Mad Hatter      | 10    | 11    | 1    |
      | Queen of Hearts | 0     | 1     | 1    |
