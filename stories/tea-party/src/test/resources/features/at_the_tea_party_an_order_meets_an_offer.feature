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
    And a generator in region <Region> has placed offers and orders
    When the tea party reads the streams of offers and orders
    Then every order for a thing on offer was settled by one transaction
    And each transaction names the order's customer, the offer's seller, and the offer's price
    And each offer was taken once, by the order that waited longest

    Examples:
      | Region |
      | EMEA   |
      | APAC   |
      | AMER   |

  Scenario Outline: An order waits until its thing is offered
    Given a tea party in region <Region>
    And a generator in region <Region> has placed orders and no offers
    When the tea party reads the streams of offers and orders
    Then nothing was settled
    When the generator places offers
    Then every order for a thing now on offer was settled

    Examples:
      | Region |
      | EMEA   |
      | AMER   |

  Scenario Outline: An offer waits until its thing is ordered
    Given a tea party in region <Region>
    And a generator in region <Region> has placed offers and no orders
    When the tea party reads the streams of offers and orders
    Then nothing was settled
    When the generator places orders
    Then every offer for a thing now ordered was taken

    Examples:
      | Region |
      | APAC   |
      | AMER   |
