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
- Open: the `FIXME` in `load-run.yaml`, `TRANSPORT` into make once `platform.mk` takes it, with
  [Messaging capability](../release-0.0.7/automation.md#messaging-capability-cli-kafka-confluent); the `kafka-*` and
  `cloudflare-*` environments filled.

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
| The Issue column in each catalog, after Feature | pair | the rows parse |
| `make project-sync`: the missing issues and milestones, each body from the phase files, the issue's milestone from the overview's file name, the issue's Phase | pair | the first run |
| `make project-links`: the catalogs committed and pushed when the sync changed them | pair | the links on the branch |
| `.github/workflows/project.yaml`: on `docs/releases/**` but `archived/**`, with `GITHUB_TOKEN`, issues and contents write | pair | the run green |
| The issue field `phase`, 2026-10-21; the board's columns by phase, its swimlanes by milestone, and its auto-add workflow on `is:issue` | themathmagician | the board shows a column per phase and a swimlane per release |
| `plan-and-supervise`: the overview's Issue column and the board | pair | the skill says what the overview holds |

## A release ships what is ready

Owner: pair.

- `@wip` and `@manual` stay out of the run, 2026-10-21: the `bdd` task at
  [platform.gradle:111](../../../platform.gradle#L111), `SCENARIOS` in [platform.mk](../../../platform.mk), and the
  README's Testing. `make bdd-snippets` played the 17 scenarios, `SCENARIOS=@nothing` none.
- Open: the root `bdd`, which parses `src/test/resources/features/` and plays none of its `@manual` scenarios.

## CI plays each transport

| Task | Owner | Turns green |
|---|---|---|
| `TRANSPORT` in `platform.mk`, `cli`, `kafka`, or `confluent`, passed to the `bdd` task, which leaves out what the transport does not carry | pair | `make bdd-snippets TRANSPORT=cli` lists the cli scenarios alone |
| The drivers on cli: the image under `local`, its log read, its console fed; the keys on `confluent` alone | themathmagician | `make bdd TRANSPORT=cli` green |
| `TRANSPORTS` and `CI_TRANSPORTS` in `platform.mk`, the reasons beside them, and `make ci-transports` | pair | `make ci-transports` prints `["cli"]` |
| `cli` in `load-run.yaml`'s choice, and its environments by `make gh-main-protection` | pair | `cli-test` and `cli-prod` listed |
| The matrix of `cicd.yaml`, job `cd`, and of `cicd-client-example.yaml`, from `make ci-transports` | pair | `cd (cli)` and `cicd-client-example (cli)` green on the pull request |
| The required checks per entry of `CI_TRANSPORTS`, added by `make gh-main-protection` to `.github/branch-protection.json` | pair, applied by themathmagician | the protection lists `cd (cli)` and `cicd-client-example (cli)` |
