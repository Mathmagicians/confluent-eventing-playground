Feature: At the tea party an order meets an offer
  The tea party is where the market meets. It reads the stream of offers and keeps the latest offer for each
  thing, the market price. It reads the stream of orders, and every order for a thing on offer is settled at
  once: a transaction from the order's customer to the offer's seller at the market price. An order for a thing
  nobody offers waits at the table until someone does.

  Background:
    Given I have the API keys to the cluster

  Scenario Outline: An order is settled at the latest offer
    Given a tea party in region <Region>
    And a generator in region <Region> has placed offers and orders
    When the tea party reads the streams of offers and orders
    Then every order for a thing on offer was settled by one transaction
    And each transaction names the order's customer, the offer's seller, and the offer's price
    And the offer is the latest one for that thing

    Examples:
      | Region |
      | EMEA   |
      | APAC   |
      | AMER   |

  Scenario Outline: An order waits until its thing is offered
    Given a tea party in region <Region>
    And a generator in region <Region> has placed orders and no offers
    When the tea party reads the streams of offers and orders
    Then no order was settled
    When the generator places offers
    Then every order for a thing now on offer was settled

    Examples:
      | Region |
      | EMEA   |
      | AMER   |
