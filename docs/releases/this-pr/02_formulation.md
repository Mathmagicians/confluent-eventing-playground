# Formulation

Each feature's scenarios and architecture rules, by link, and the step skeletons, for the features of the
[catalog](release-spec-0.0.6.md) that passed Examples agreed. The evidence of Spec signed, the red run, is linked from
the feature's row in the catalog.

## Load on demand: the transport and its environment

| Task | Owner | Done when |
|---|---|---|
| The spec: the Git and CI bullet on `load-run.yaml` in the README, and `.github/environment-test.json` and `environment-prod.json`, applied by `make gh-main-protection` | pair | themathmagician signs it |
| The red run: a dispatch from `eventing-without-confluent` with `transport=kafka`, in the empty `kafka-test` | pair | the run shown, red at startup |

Spec signed 2026-10-21. The run,
[39508585975](https://github.com/Mathmagicians/wonderland-eventing-playground/actions/runs/39508585975), from
`eventing-without-confluent` with `transport=kafka`, deployed to `kafka-test` with `ENV=test` and skipped the load:
the registry holds no `latest` under the new image name until `main` builds it.

## Project sync: the board follows the catalog

Spec signed 2026-10-21. The spec is
[the_board_follows_the_catalog.feature](../../../src/test/resources/features/the_board_follows_the_catalog.feature),
`@manual`, checked on the first runs of `project.yaml`.

| Task | Owner | Done when |
|---|---|---|
| The map's scenarios in `the_board_follows_the_catalog.feature`, `@manual` | pair | each example of the map is a scenario |
| The first push with the Issue cells empty | pair | the run shows the issues created and the links pushed |
| A gate signed and pushed | pair | the card is in the row's new state |

## The catalog follows the board

| Task | Owner | Done when |
|---|---|---|
| The map's scenarios in [the_catalog_follows_the_board.feature](../../../src/test/resources/features/the_catalog_follows_the_board.feature), `@manual` | pair | each example of the map is a scenario |

## A release ships what is ready

| Task | Owner | Done when |
|---|---|---|
| The map's scenarios in [a_release_ships_what_is_ready.feature](../../../src/test/resources/features/a_release_ships_what_is_ready.feature), `@manual` | pair | each example of the map is a scenario |

## Messaging capability: cli, kafka, confluent

| Task | Owner | Done when |
|---|---|---|
| The map's scenarios in `platform/src/test/resources/features/i_can_pick_where_a_story_plays.feature` | themathmagician | each example of the map is a scenario |
| An architecture rule: the Kafka adapters carry `@Profile("kafka")`, the console and log adapters `@Profile("cli")` | themathmagician | it fails on `@Profile("!local")` |
| Step skeletons for the new steps, each throwing `PendingException` | pair | `make bdd-snippets` finds every step defined |

## CI plays each transport

| Task | Owner | Done when |
|---|---|---|
| `@confluent` on the cluster's and the Flink table's features, with the Background on them alone; `@kafka` on the scenarios about partitions, offsets, and the registry | themathmagician | `make bdd-snippets TRANSPORT=cli` lists none of them |
| The cli scenarios of the generator, the tea party, and the purse, from the map | themathmagician | each example of the cli rule is a scenario |
