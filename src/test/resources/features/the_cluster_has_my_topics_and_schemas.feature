Feature: The cluster has my topics and schemas
  One topic per payload type, its dead-letter twin, and the Protobuf schema registered for each topic. Terraform
  creates them from iac/, so a generator never does.

  Background:
    Given I have the API keys to the cluster

  Scenario Outline: A topic exists with its schema
    When I look up the topic <Topic>
    Then it exists, and so does its dead-letter topic <Topic>.DLT
    And the Protobuf schema schemas.proto is registered for <Topic>
    And that schema describes the envelope and its <Payload>
    And the schema evolves with BACKWARD compatibility

    Examples:
      | Topic        | Payload     |
      | products     | Product     |
      | offers       | Offer       |
      | orders       | Order       |
      | transactions | Transaction |
