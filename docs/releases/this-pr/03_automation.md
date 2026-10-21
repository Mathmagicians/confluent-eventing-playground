# Automation

The tasks of each feature in the [catalog](release-spec-0.0.6.md), each with its owner and the scenario it turns
green. One go per change. The evidence of Spec green, the green run or what themathmagician saw, and the ADRs a
feature touches, is linked from the feature's row in the catalog.

## Load on demand: the transport and its environment

Owner: pair. Turns green: the dispatches of the map.

- [load-run.yaml](../../../.github/workflows/load-run.yaml): `workflow_dispatch` only; `transport` and the branch
  pick `<transport>-<test|prod>`, prod from `main`, 2026-10-20.
- The environments: `TRANSPORTS` in the [Makefile](../../../Makefile), `.github/environment-test.json` and
  `environment-prod.json`, applied by `make gh-main-protection`, 2026-10-20.
- `TRANSPORT` reaches make, [load-run.yaml:28](../../../.github/workflows/load-run.yaml#L28), and `platform.mk`
  plays it, 2026-10-22.
- Open: Spec green after the merge, when `main` builds `latest`; the `kafka-*` and `cloudflare-*` environments
  filled.

## Slides spike: the talk plays from compose

Owner: pair.

- The talk plays in the `slides` service of [compose.yaml](../../../compose.yaml), from
  [slides/Dockerfile](../../../slides/Dockerfile); Node and Chromium live in the image. `make slides-present` opens
  the browser, `make slides-pdf` exports.
- One compose plays Wonderland and the talk, signed 2026-10-20.
- Spec green, signed 2026-10-20: themathmagician played the talk, "y it works nicely", and `make slides-pdf` wrote
  `docs/generated/slides/architecture-discoverability.pdf` from the container.

## One name: wonderland-eventing-playground

Owner: pair; the repository renamed by themathmagician. Turns green: the rule of the map.

- The repository `Mathmagicians/wonderland-eventing-playground`, renamed 2026-10-21.
- The spellings, 2026-10-21: the badges in the [README](../../../README.md) and
  [examples/purse/README.md](../../../examples/purse/README.md), `IMAGE` in the [Makefile](../../../Makefile),
  [compose.yaml](../../../compose.yaml), [platform.gradle:45](../../../platform.gradle#L45),
  [architecture-discovery/build.gradle:57](../../../architecture-discovery/build.gradle#L57),
  [settings.gradle:1](../../../settings.gradle#L1),
  [app/.../application.properties:1](../../../app/src/main/resources/application.properties#L1), and
  `EventFixtures.APP`.
- Spec green, signed 2026-10-21: `grep -rIi confluent-eventing-playground` finds no file outside the build output,
  and `make build` is green.
- Open: `origin` at the new name, the Terraform Cloud workspace's VCS connection, and the visibility of the new
  ghcr package after the first push.

## Project sync: the board follows the catalog

| Task | Owner | Turns green |
|---|---|---|
| The Issue column in each catalog, after Feature | pair | done 2026-10-21: every row of 0.0.6, 0.0.7, and 0.0.8 linked |
| `make gh-project-sync`: the missing issues and milestones, each body from the phase files, the issue's milestone from the overview's file name, the issue's Phase | pair | done 2026-10-21: the first run created issues #7 to #19 and the milestones 0.0.6, 0.0.7, and 0.0.8 |
| The issue field `phase`, 2026-10-21; the board's columns by phase, its swimlanes by milestone, and its auto-add workflow on `is:issue` | themathmagician | done 2026-10-21: the board shows a column per phase and a swimlane per release |
| `plan-and-supervise`: the overview's Issue column and the board | pair | done 2026-10-22: the skill says what the overview holds and when the board is synced |

Spec green 2026-10-22: `make gh-project-sync`.

## CI plays each transport

| Task | Owner | Turns green |
|---|---|---|
| `TRANSPORT` in `platform.mk`, default `cli`, passed to the `bdd` task with the profiles `<transport>,<env>`; the task leaves out what the transport does not carry | pair | done 2026-10-22: `make bdd-snippets` plays 3 + 7 scenarios, `TRANSPORT=confluent` 10 + 7 |
| `TRANSPORTS` and `CI_TRANSPORTS` in `platform.mk`, the reasons beside them, and `make ci-transports` | pair | done 2026-10-21: `make ci-transports` prints `["cli"]` |
| The drivers' port [Transport](../../../platform/src/testFixtures/java/dk/mathmagicians/playground/confluent/eventing/bdd/Transport.java), one bean per transport: `ClusterTransport` on `confluent`, the cluster and registry calls the drivers made before; `ConsoleTransport` on `cli`, the story's console typed into, the image's log read | pair | `make bdd` green; `make bdd TRANSPORT=confluent` green once the cluster answers |
| `LoggingPublisher` logs the receipt, the key, the region, and the payload as the console reads it, `Converter.text` | pair | done 2026-10-22: `LoggingPublisherTest`, `ConverterTest`; `make run-tiny` logs `Published ENVL-… to log-0 at offset 0, key EMEA/P-DRIN, region EMEA: Offer offer_id: "OFF-…" product_id: "P-DRIN" …` |
| `cli` in `load-run.yaml`'s choice, and its environments `cli-test` and `cli-prod` | pair | GitHub creates them on first use; `make gh-main-protection` deploys `cli-prod` from `main` alone |
| The `bdd` job of `cicd.yaml`, a matrix over `make ci-transports`, before `cd`, which publishes the image; `cicd-client-example.yaml` plays the purse in `cli-test` | pair | `bdd (cli)`, `cd`, and `cicd-client-example` green on the pull request |
