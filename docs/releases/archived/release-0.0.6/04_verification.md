# Verification

The release gate, **In production**: every feature the [catalog](release-spec-0.0.6.md) ships Spec green, the release
checks passed, and Wonderland playing in each of its environments.

## Release checks

| Check | Evidence | Signed |
|---|---|---|
| `make check` green | the sequence of `make check` as the merge's run [39692749878](https://github.com/Mathmagicians/wonderland-eventing-playground/actions/runs/39692749878) played it: `generated-check`, `arch-verify`, `build`, `docker-image`, `bdd-published` on `cli` | 2026-10-23 |
| `ci`, `cd`, `iac`, `architecture-discovery`, `cicd-client-example` green on the PR | the branch's last push: cicd [39691461043](https://github.com/Mathmagicians/wonderland-eventing-playground/actions/runs/39691461043), iac [39691461036](https://github.com/Mathmagicians/wonderland-eventing-playground/actions/runs/39691461036), architecture-discovery [39691461011](https://github.com/Mathmagicians/wonderland-eventing-playground/actions/runs/39691461011), cicd-client-example [39691460970](https://github.com/Mathmagicians/wonderland-eventing-playground/actions/runs/39691460970); the same four green on `main` after the merge | 2026-10-23 |
| Review findings above Nit resolved, or deferred with a reason | deferred to 0.0.7: the console's region check and `toUpperCase`, Messaging capability; `spotlessApply`, README | 2026-10-23 |
| README, `make help`, and `docs/adr.md` say what the code does | the README's Git and CI on the `bdd` job per transport, `make help` listing `ci-transports`, ADR-001 accepted 2026-10-22 | 2026-10-23 |

## ADR compliance

| ADR | Checked by | Evidence |
|---|---|---|
| [ADR-001 CI plays each transport in a matrix](../../adr.md#adr-001-ci-plays-each-transport-in-a-matrix) | `make ci-transports`, and review of `cicd.yaml` and `cicd-client-example.yaml` | `make ci-transports` prints `["cli"]`, 2026-10-23; the `bdd` job's matrix reads it, [cicd.yaml:42](../../../.github/workflows/cicd.yaml#L42); the purse plays in `cli-test`, run [39691460970](https://github.com/Mathmagicians/wonderland-eventing-playground/actions/runs/39691460970) |

## In production

| Environment | Tag | Image | Wonderland plays | Signed |
|---|---|---|---|---|
| `test` | `v0.0.6` | `ghcr.io/mathmagicians/wonderland-eventing-playground:0.0.6` | `bdd (cli)` in `cli-test`, the merge's run [39692749878](https://github.com/Mathmagicians/wonderland-eventing-playground/actions/runs/39692749878) | 2026-10-23 |
| `prod` | `v0.0.6` | `ghcr.io/mathmagicians/wonderland-eventing-playground:latest` | `make docker-smoke` in `cli-prod`, the dispatch [39694484209](https://github.com/Mathmagicians/wonderland-eventing-playground/actions/runs/39694484209) | 2026-10-23 |

Confluent Cloud answers its keys with `SaslAuthenticationException` since 2026-09-29, so `confluent` plays in neither
environment: 0.0.7, Messaging capability.
