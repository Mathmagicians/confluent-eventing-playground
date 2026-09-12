---
name: voice
description: How the team's files read and the words they use. Use when writing or reviewing anything that lives in the repository, README, comments, Makefile help, feature files, workflow names, and when a document or a diagram is described.
---

# Voice

Every file outside `.claude/` reads as written by the team, once, in the present tense, stating what we do.

## The rules

- State what we do, phrased as the practice we follow. No history, no "we decided to", no "currently".
- Plain statements. An adjective stays when it carries information: `idempotent`, `unchecked`. No "simple", no
  "robust", no "nice".
- Once. A fact lives in its home, see `/dry`, and every other place refers to it by name. No remarks about where
  a version comes from, no tool names in the team's files, no restated rules in comments.
- Agents, Claude, and `CLAUDE.md` are mentioned in `.claude/` and `CLAUDE.md` only.
- A legend explains a diagram's arrows. Prose points at the legend in one line and never restates it.
- The README is the human's: voice, structure, wording. Plumbing that changes what it says hands over the lines
  to add, a table row, a command, a variable name, and the human places them. Written into the README only on
  "you do it".
- A `##` comment on every Makefile target, since `make help` is built from them; a `##@` line per section says
  what the section is.
- A feature file describes one capability in domain language. Topics, ordering, and schemas are domain concepts
  here; partition counts, class names, and client configuration are not.

## The words

| We say | Not |
|---|---|
| the platform | the framework, the library |
| a story | a service, a consumer, a job |
| the flock | the swarm, which is Docker's |
| the generator | the producer, the load generator |
| the Flink cluster in the cloud | the cloud, Flink |
| a table set up with IaC | a table Flink declared |
| the tea party settles | Flink settles |
| `test.orders`, the environment first, the dot the only separator | `orders-test`, `orders_test` |
| the dead-letter topic `<topic>.DLT` | the DLQ |
| keys are for ordering, headers are for stream processing | keys for filtering |
| Powered by Confluent Cloud ... and Terraform Cloud | any other caption under the two pictures |
