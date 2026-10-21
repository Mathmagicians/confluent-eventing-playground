# Formulation

Each feature's scenarios and architecture rules, by link, and the step skeletons, for the features of the
[catalog](release-0.0.7.md) that passed Examples agreed. The evidence of Spec signed, the red run, is linked from the
feature's row in the catalog.

## Messaging capability: cli, kafka, confluent

| Task | Owner | Done when |
|---|---|---|
| The map's scenarios in `platform/src/test/resources/features/i_can_pick_where_a_story_plays.feature` | themathmagician | each example of the map is a scenario |
| An architecture rule: the Kafka adapters carry `@Profile("kafka")`, the console and log adapters `@Profile("cli")` | themathmagician | it fails on `@Profile("!local")` |
| Step skeletons for the new steps, each throwing `PendingException` | pair | `make bdd-snippets` finds every step defined |

## The catalog follows the board

| Task | Owner | Done when |
|---|---|---|
| The map's scenarios in [the_catalog_follows_the_board.feature](../../../src/test/resources/features/the_catalog_follows_the_board.feature), `@manual` | pair | each example of the map is a scenario |

## A release ships what is ready

| Task | Owner | Done when |
|---|---|---|
| The map's scenarios in [a_release_ships_what_is_ready.feature](../../../src/test/resources/features/a_release_ships_what_is_ready.feature), `@manual` | pair | each example of the map is a scenario |

## Stories describe themselves

| Task | Owner | Done when |
|---|---|---|
| An architecture rule: every `Story` carries `@PrimaryPort` with a description | themathmagician | it fails on today's stories |
| Unit tests for each story's `publishes()` | themathmagician | they fail on today's `Story` |

## Architecture discovery: one model, three levels

| Task | Owner | Done when |
|---|---|---|
| A test of the `model.dsl` written from a fixture, in architecture-discovery | themathmagician | it fails on today's writer |
| `make arch-check`, exact | pair | it fails on a reordered line |

## The skill generate-platform-client

| Task | Owner | Done when |
|---|---|---|
| The client's features and architecture classes, the acceptance it turns green | themathmagician | they run red on an empty client |
