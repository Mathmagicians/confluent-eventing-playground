# Terraform with Confluent Cloud

## Provider and workspace

- Provider `confluentinc/confluent`. One cluster serves every environment; the environment is the topic prefix.
- Single-cluster mode: the provider block takes `kafka_id` (`lkc-…`), `kafka_rest_endpoint` (`https://…:443`, not
  the bootstrap server), `kafka_api_key`, `kafka_api_secret`, and the same four for the registry, `schema_registry_id`
  (`lsrc-…`), `schema_registry_rest_endpoint`, and its own key pair. A Kafka key does not open the registry.
- The values are Terraform variables of the workspace, declared in `variables.tf` with a validation on the
  endpoints. Names are case-sensitive: a workspace variable `KAFKA_REST_ENDPOINT` never reaches
  `var.kafka_rest_endpoint`. Declared variables fail at plan time with their names; provider-level environment
  variables fail at apply time inside the provider.
- `cloud {}` stays empty; `TF_CLOUD_ORGANIZATION` and `TF_WORKSPACE` name the workspace, from `.env.test.private`
  locally and the `terraform-cloud` GitHub environment in CI. The token is `terraform login` once locally,
  `TF_TOKEN_app_terraform_io` in CI, the CLI's rule `TF_TOKEN_<hostname>`.
- A git-triggered workspace refuses a CLI apply. `make tf-plan` is a speculative plan, the apply is the merge to
  `main`. `make tf-check` depends on `tf-init`, so it needs the workspace too.

## Topics

- `locals` hold the names and the environments once; `main` is the set of service topics, `topics` adds a `.DLT`
  twin to each. One `confluent_kafka_topic` with `for_each`.
- The dead-letter twin keeps the partition count of its source: Spring's recoverer publishes to the same partition
  number. `.DLT` is Spring's default suffix.
- Confluent Cloud never auto-creates topics, so the dead-letter topics exist before the first failure.

## Schemas

- One proto file per payload, registered under the topic's `<topic>-value` subject, `TopicNameStrategy`. The
  registry stores the canonical form, imports before options, two-space indent, so a comparison canonicalizes both
  sides.
- A file that imports another needs `schema_reference` blocks: `name` is the import's filename, `subject_name` and
  `version` come from the referenced resource. The referencing resource sets `skip_validation_during_plan = true`,
  because the referenced versions are unknown while the plan creates them in the same run; the registry validates on
  apply. A resource cannot reference its own `for_each` instances, so the referencing schema is its own resource.
- Removing a message from a subject's schema is not backward compatible, `MESSAGE_REMOVED`. Splitting one file into
  several means deleting the subjects and registering afresh, fine while no data used them.
- `confluent_schema_registry_cluster_config` pins `BACKWARD` once, the registry's default made explicit.
- Outputs: a map of topic to partition count, a map of subject to version, from `concat(values(...))` over the
  schema resources.
