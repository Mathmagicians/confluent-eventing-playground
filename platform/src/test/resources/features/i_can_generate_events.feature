Feature: I can publish messages
  A generator in a region puts a payload in an envelope and publishes it as a message to the payload's topic.

  Background:
    Given I have the API keys to the cluster

  Scenario Outline: I can publish a message
    Given I am a generator in region <Region>
    When I publish one <Payload> in an envelope
    Then I receive the envelope id, and the partition and offset the message landed on
    And the message at that offset in the topic <Topic> is my envelope
    And the message is serialized with the protobuf schema registered for the topic <Topic>
    And the envelope names region <Region>

    Examples:
      | Region | Payload | Topic    |
      | EMEA   | Order   | orders   |
      | APAC   | Offer   | offers   |
      | AMER   | Product | products |
