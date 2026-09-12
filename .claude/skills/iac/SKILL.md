---
name: iac
description: Terraform with Confluent Cloud as this repository does it. Use for anything under iac/ and src/main/flink, the Terraform Cloud workspace and its variables, topics, schemas, Flink tables and statements, the lab in the SQL workspace before the code, and the errors this stack answers with. CLAUDE.md holds the agreement, /dry the homes, /architect-pair how a step runs; this skill is the Terraform and Flink playbook.
---

# IaC, Terraform with Confluent Cloud

Terraform Cloud creates the topics, the schemas, and the Flink tables from `iac/`, applied on every push to `main`.
The plan is the review, the human approves it, and nothing in the code second-guesses either.

## The workspace

- VCS-driven on `main`. A CLI apply is refused; `make tf-plan` is a speculative plan, the apply is the merge.
  `make tf-check` depends on `tf-init`, so it needs the workspace too.
- `cloud {}` stays empty; `TF_CLOUD_ORGANIZATION` and `TF_WORKSPACE` name the workspace, from `.env.test.private`
  locally and the `terraform-cloud` GitHub environment in CI. The token is `terraform login` once locally,
  `TF_TOKEN_app_terraform_io` in CI, the CLI's rule `TF_TOKEN_<hostname>`.
- Data plane only. The workspace holds the cluster, the registry, the environment, the compute pool, the principal,
  and the keys as Terraform variables, declared in `variables.tf` with a validation on each id. It never mints keys,
  never creates accounts, never imports. The Cloud key is for read-only lookups, `make confluent-lookup`, and for
  nothing in the workspace.
- One key per plane: a Kafka key does not open the registry, a Flink key is scoped to one region, `gcp.europe-north1`
  here, and a key of another region answers 404. The Cloud key answers 401 where a Flink key belongs and the reverse.
- Variable names are lowercase and case-sensitive: a workspace variable `KAFKA_REST_ENDPOINT` never reaches
  `var.kafka_rest_endpoint`. Declared variables fail at plan time with their names; provider-level environment
  variables fail at apply time inside the provider.
- `TF_LOG_PROVIDER` is an environment variable of the workspace, category Environment variable, never a Terraform
  variable and never a `variable` block: as a Terraform variable it is "undeclared" on every run and switches nothing
  on. `INFO` is one line per API call, `DEBUG` for the day something 400s. Runs execute remotely, so a local value
  never reaches the provider.
- A workspace variable pasted with a trailing newline, `"u-2rz7xpo\n"`, is a 400 on every statement. `TF_LOG_PROVIDER`
  shows the body.

## The rules

- One resource per concept, `for_each` over the `locals`: the names and the environments live there once, every
  resource file builds from them, never a second list.
- Outputs are the facts `make` reads: the cluster, the registry, the topics, the schema versions, the compute pool
  with its endpoint, principal, catalog and database, and per environment the Flink tables and statements by name.
- No lifecycle brakes. No `prevent_destroy`, no `ignore_changes`, no `replace_triggered_by`. A destructive change is
  named in the proposal in one line and decided in the plan review. `terraform destroy` followed by an apply rebuilds
  everything in order, and that is tested, not assumed.
- A one-shot DDL is a plain `confluent_flink_statement`, created with the topics, gone with them on a destroy. An edit
  re-runs it, and a re-run against what already exists is a visible red apply, which is right.
- Data sources for what exists and is not ours: the organization, the environment, the cluster, the compute pool, the
  region. The pool's cloud and region give the Flink REST endpoint, the environment's and the cluster's display
  names give the catalog and the database a statement resolves names in.

## Topics

- `locals` hold the names and the environments once; `main` is the set of service topics, `topics` adds a `.DLT`
  twin to each. One `confluent_kafka_topic` with `for_each`.
- The dead-letter twin keeps the partition count of its source: Spring's recoverer publishes to the same partition
  number. `.DLT` is Spring's default suffix.
- Confluent Cloud never auto-creates topics, so the dead-letter topics exist before the first failure.
- A topic is replaced on a new name, a new cluster, or a partition decrease; everything else, a partition increase
  included, updates in place.

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
- `hard_delete = true` while nothing is live, so a destroy removes the subject for real and a rebuild starts clean.

## Flink

- A table is a topic with a subject: the catalog is the environment's name, the database the cluster's, the table
  the topic's, and the dot inside `test.products` forces backticks. Columns are the Protobuf fields plus `$rowtime`
  and `key`. Headers are not inferred: `ALTER TABLE ... ADD (headers MAP<BYTES, BYTES> METADATA VIRTUAL)` once per
  table, `VIRTUAL` keeps it out of the subject. A query reads a header with
  `DECODE(headers[ENCODE('ce_region', 'UTF-8')], 'UTF-8')`, the map is keyed by bytes.
- The SQL is source, under `src/main/flink`, one file per table or statement, the `SELECT` alone for a table, no
  trailing semicolon. `templatefile` fills `${env}`, the one template variable, comments included; a literal `${`
  is `$${`, and `$rowtime` passes through.
- `confluent_flink_materialized_table` carries what the DDL carries: `display_name`, `query`, `distribution` with
  `kind = "HASH"`, the keys and the bucket count, `constraints` with `type = "PRIMARY_KEY"` on the same keys and
  `enforced = false`, `table_options` for the `WITH` clause, `session_options` for catalog and database, and
  `kafka_cluster`. Upsert mode needs the bucket key equal to the primary key, and a primary key column cannot be
  nullable, so a header column is wrapped in `COALESCE`.
- A `GROUP BY` query keeps one row per key and updates it: `LAST_VALUE` for the latest fields, `COUNT(*)` for the
  versions, `MAX($rowtime)` for when. `at` is a reserved word.
- Verification is by name. `flink-verify` reads each object Terraform declared, from `tf output flink`: a table under
  `databases/<cluster id>/materialized-tables/<name>`, a statement under `statements/<name>`. The statements list is
  paged by ten and sorted by name, so it never proves anything about ours. A `LIMIT` never bounds a query over an
  upsert table, so no target waits for one to complete.
- `make flink-statements` is the raw listing, `make confluent-lookup` the ids the workspace variables want.

## The lab before the code

The SQL workspace in the console is where a query is tried: run the `SELECT` and watch it update while
`make up-product` produces, then `CREATE MATERIALIZED TABLE` under a lab name, never the name Terraform will use,
query it like a table, `SHOW CREATE MATERIALIZED TABLE` to read it back, `DROP MATERIALIZED TABLE` before the apply.
An `ALTER` run by hand in the lab is in the way of the apply: drop it, or tear down and let Terraform add it.

## Errors this stack answers with

| Answer | Cause |
|---|---|
| 404 creating a statement | the Flink key is of another region |
| 401 on the management API | a Flink key where the Cloud key belongs, or the reverse |
| 400 with the principal in the body | a trailing newline in the workspace variable |
| `Column 'headers' not found` | the `ALTER` did not run on that environment's table |
| `Encountered "<EOF>"` expecting `SELECT` | the SQL file holds comments only |
| `Cannot apply 'ITEM'` with `CHAR` | a string literal against the bytes-keyed headers map |
| `All 7 flink_* ... at the same time` | Flink settings on the provider block, partial; the statements carry them |
| `Value for undeclared variable` | a Terraform variable that should be an environment variable |
| `MESSAGE_REMOVED` | a subject's schema lost a message; delete the subject |
| `InvalidVersionException: 0` | a reference to a version created in the same plan; skip validation during plan |
