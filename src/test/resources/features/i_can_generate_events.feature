Feature: I can publish messages
  A generator puts a payload in an envelope and publishes it as a message to the topic.

  Background:
    Given I have the API keys to the cluster

  Scenario Outline: I can publish a message
    Given I am the generator <Generator> in region <Region>
    When I publish one <Payload> in an envelope
    Then I receive the envelope id
    And the partition and offset the message landed on
    And the message at that offset is in the topic
    And its envelope names <Generator> and <Region>

    Examples:
      | Generator | Region | Payload |
      | Alice     | EMEA   | Order   |
      | Cat       | APAC   | Offer   |
