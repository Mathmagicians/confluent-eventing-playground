Feature: The cluster has my topics and schemas
  One topic per payload type, its dead-letter twin, and the Protobuf schema registered for each topic. Terraform
  creates them from iac/, so a generator never does.

  Background:
    Given I have the API keys to the cluster

  Scenario Outline: A topic exists with its schema
    Then the topic <Topic> exists
    And the dead-letter topic for <Topic> exists
    And the Protobuf schema <Schema> is registered for the topic <Topic>
    And that schema describes the message <Payload>
    And the schema evolves with BACKWARD compatibility

    Examples:
      | Topic        | Payload     | Schema            |
      | products     | Product     | product.proto     |
      | offers       | Offer       | offer.proto       |
      | orders       | Order       | order.proto       |
      | transactions | Transaction | transaction.proto |

  Scenario Outline: A Flink table set up with IaC is running, with its topic
    Then the Flink cluster in the cloud runs my table <Table>
    And the topic <Table> exists

    Examples:
      | Table           |
      | product_catalog |
