# Architecture decisions

The decisions that shape the architecture, one entry each, numbered in the order they are written. An entry keeps its
text; a decision that replaces it is a new entry, and the earlier entry's status names it.

## Format

```
### ADR-NNN Title

- **Date:** the day of its status
- **Status:** Proposed, Accepted, or Superseded by ADR-NNN
- **Context:** the facts that call for the decision
- **Decision:** what we do
- **Consequences:** what follows from it
- **Enforced by:** the architecture rule or test that checks it, or review
```

## Decisions

### ADR-001 CI plays each transport in a matrix

- **Date:** 2026-10-22
- **Status:** Accepted
- **Context:** A process plays a transport beside its environment, `<transport>,<env>`. Confluent Cloud answers the
  keys of `confluent-test` and `confluent-prod` with `SaslAuthenticationException` since 2026-09-29, and a run on it
  is billed. Branch protection requires `ci`, `cd`, `iac`, `architecture-discovery`, and `cicd-client-example` by
  name, and a job with a matrix reports a check per entry, `bdd (cli)`.
- **Decision:** The `bdd` job of `cicd.yaml` plays the features against the candidate once per transport of
  `CI_TRANSPORTS` in `platform.mk`, read through `make ci-transports`, each entry in its `<transport>-test`
  environment. `cd` follows `ci` and `bdd` and publishes the tested candidate. A transport that sits out carries its
  reason beside `CI_TRANSPORTS`: `confluent` for Confluent Cloud's pricing, `kafka` until its broker and schema
  registry run on the runner, `cloudflare` until it plays. Each transport plays the scenarios it carries; a feature
  that only one transport carries is tagged with it, `@confluent`.
- **Consequences:** `ci` and `cd` keep the names branch protection requires. A transport enters CI by its name in
  `CI_TRANSPORTS`. On `cli` the features of the cluster and the Flink table sit out; they play on `confluent` from a
  developer machine. `cicd-client-example.yaml` plays the purse on `cli` in `cli-test`.
- **Enforced by:** `make ci-transports`, and review of `cicd.yaml` and `cicd-client-example.yaml`.
