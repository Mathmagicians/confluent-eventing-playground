Feature: The Flink cluster in the cloud keeps the product catalog current
  Every product record is a new version of the same thing: the name stays, the description and the producer are
  fresh. The catalog keeps each product once, as it was last described, and counts its versions. Terraform declares
  the catalog from iac/, a materialized table over the products topic; no story is involved.

  Background:
    Given I have the API keys to the cluster

  Scenario Outline: The catalog shows the latest version and counts them
    Given a generator in region <Region> has placed products
    And the generator has placed products again
    Then the Flink cluster in the cloud shows each product once, in its latest version, with its versions counted

    Examples:
      | Region |
      | EMEA   |
