---
name: dry
description: The constitution of this repository. Every fact has one home and everything else refers to it by name. Use before adding, reviewing, or documenting anything, and when deciding where a value, a rule, a step, a resource, or a document belongs.
---

# DRY, one place to rule them all

Every fact has one home. Everything else refers to it by name, never by copy. A second copy of the same knowledge
is the trigger to extract, and the question before writing anything is: where does this already live?

## The homes

| Fact | Its one home | Who refers to it |
|---|---|---|
| Variable names | `.env.private.sample` | Makefile, `compose.yaml`, workflows, properties, test drivers, by name |
| Credentials | `.env.<ENV>.private`, GitHub environments, the Terraform Cloud workspace | `make` sources the file into the command it runs; nothing else reads it |
| Kafka client settings | the `kafka` profile, `application-kafka.properties` | the app, the admin client, the consumer, the registry client, through `KafkaProperties` |
| Topic per payload | `topics.*` in the profile | `Topics`, features by the plain word, `ParameterTypes` resolves it |
| The schema | the proto file under `src/main/proto` | protoc, Terraform, the BDD comparison |
| Stream processing | the SQL under `src/main/flink` | Terraform submits it, `${env}` the one template variable |
| The version | the git tag | Gradle, `make version`, the image tag, the release |
| How anything runs | the Makefile | humans, every workflow step, compose, the README's commands |
| Environments and names | `locals` in `iac/` | every `for_each`, never a second list |
| What we do and how | README | `CLAUDE.md` and the skills point at it and never restate it |
| Generated code and documents | their generator | committed output; `proto-check`, `arch-check`, `generated-check` catch drift |
| History | git, written by the human | agents read: `status`, `diff`, `log`, `show` |

## Off limits for agents

- `.env.*.private`: never read, never opened, never sourced. `make` carries the values into the one command that
  needs them, and that is the whole contact.
- Git writes: no `add`, `commit`, `push`, `pull`, `checkout`, `rebase`, `tag`, `stash`, `config`, no branch
  changes. An agent names the paths to stage and stops there.
- `.claude/settings.json`: the human's guard. A rule that blocks a task is a finding, never an edit.

## Unavoidable copies

Across languages and tools a copy cannot be a reference. The `CREDENTIALS` list exists in the Makefile and in
`GeneratorContainer`, the topic names exist in `iac/` and in the profile properties. The copy names its source in a
comment, and where a check can catch drift, it does: generated output is committed and checked, not regenerated
silently.

## DRY reads before it writes

- `grep` for the pattern before declaring it new.
- Search for an existing step before writing one, and for a parameter type before a second step text.
- A `for_each` over a map before a second resource with the same body.
- A rule in the README before a rule in a comment, a comment before a rule in the chat.

## DRY shapes the architecture

Followed all the way it yields the hexagon. The domain is the one place for rules. A port is the one place a need
is stated. An adapter is the one place a technology is spoken. Duplication across adapters is a missing port,
duplication across use cases a missing domain type, duplication across services a missing shared module.

## In review

A DRY breach is Major, cited with both locations and the home the knowledge should have.
