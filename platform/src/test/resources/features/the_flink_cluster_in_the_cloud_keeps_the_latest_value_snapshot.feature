Feature: The Flink cluster in the cloud keeps the latest value snapshot of the products
  Every product record is a new version of the same thing: the name stays, the description and the producer are
  fresh. The snapshot, products.lvs, keeps each product once per region, as it was last described, and counts its
  versions. Terraform sets it up from iac/, a materialized table over the products topic keyed by region and
  product, the region read from the message's headers; no story is involved.

  Background:
    Given I have the API keys to the cluster

  Scenario Outline: The snapshot shows the latest version per region and counts them
    Given a generator in region <Region> has placed products
    And the generator has placed products again
    Then the Flink cluster in the cloud shows in table products.lvs each product of region <Region> once, in its latest version, with its versions counted

    Examples:
      | Region |
      | EMEA   |
      | APAC   |
