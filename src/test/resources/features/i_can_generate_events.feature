Feature: I can generate events that simulate orders of different products from different customer factories, and publish them as events

    Background:
    Given I have the API keys to the event log
    And I can access the schema registry

    Scenario Outline: I can put in a request for <Entity> from my factory
    Given I am the factory <Factory> in region <Region>
    And I need a new <Entity> with frequency <Frequency> per second
    When I put in a request for <Entity>
    Then it is published as an event and I receive an event_id
    And I can see an event with my event_id in the event log
    And the event has a header with my factory <Factory> and region <Region>

    Examples:
    | Factory    | Region | Entity | Frequency |
    | Alice      | EMEA   | Order  | 1         |
    | Wonderland | ASIA   | Order  | 4         |
    | Cat        | EMEA   | Prices | 1000      |
