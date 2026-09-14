Feature: At the tea party an order meets an offer
  The tea party is where the market meets. An offer is one thing for sale, an order one thing wanted. The tea
  party reads both streams, and an order for a thing on offer is settled at once: a transaction from the order's
  customer to the offer's seller at the offer's price, and the offer is taken. An order for a thing nobody offers
  waits at the table until someone does, an offer nobody wants waits for an order, each served in the order it
  came.

  Background:
    Given I have the API keys to the cluster

  Scenario Outline: An order is settled with an offer for its thing
    Given a tea party in region <Region>
    And Mad Hatter offers a Top Hat for 12.5 coins
    And March Hare offers a Tea Set for 3 coins
    And Alice orders a Top Hat
    And Dormouse orders a Tea Set
    When the tea party reads the streams of offers and orders
    Then for every order the tea party read there is a transaction, the order was settled
    And Alice paid Mad Hatter 12.5 coins for the Top Hat
    And Dormouse paid March Hare 3 coins for the Tea Set

    Examples:
      | Region |
      | EMEA   |
      | APAC   |
      | AMER   |

  Scenario Outline: An order waits until its thing is offered
    Given a tea party in region <Region>
    And Alice orders a Top Hat
    When the tea party reads the stream of orders
    Then nothing was settled
    When Mad Hatter offers a Top Hat for 12.5 coins
    And the tea party reads the stream of offers
    Then Alice paid Mad Hatter 12.5 coins for the Top Hat

    Examples:
      | Region |
      | EMEA   |
      | AMER   |

  Scenario Outline: An offer goes to the order that waited longest
    Given a tea party in region <Region>
    And Alice orders a Top Hat
    And White Rabbit orders a Top Hat
    And Mad Hatter offers a Top Hat for 12.5 coins
    When the tea party reads the streams of offers and orders
    Then Alice paid Mad Hatter 12.5 coins for the Top Hat
    And White Rabbit is still waiting for a Top Hat

    Examples:
      | Region |
      | APAC   |
      | AMER   |
