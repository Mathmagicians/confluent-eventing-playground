# Formulation · 0.0.6

Each idea's scenarios and architecture rules, by link, and the step skeletons, for the ideas of the
[catalog](release-spec-0.0.6.md) whose rules passed Examples agreed. The evidence of Spec signed, the red run, is linked
from the rule's row in the catalog.

## Messaging capability: cli, kafka, confluent

| Task | Owner | Done when |
|---|---|---|
| The map's scenarios in `platform/src/test/resources/features/i_can_pick_where_a_story_plays.feature` | themathmagician | each example of the map is a scenario |
| An architecture rule: the Kafka adapters carry `@Profile("kafka")`, the console and log adapters `@Profile("cli")` | themathmagician | it fails on `@Profile("!local")` at [KafkaPublisher.java:26](../../../platform/src/main/java/dk/mathmagicians/playground/confluent/eventing/adapter/kafka/publisher/KafkaPublisher.java#L26) |
| Step skeletons for the new steps, each throwing `PendingException` | pair | `make bdd-snippets` finds every step defined |

## CI off Confluent

| Task | Owner | Done when |
|---|---|---|
| `@confluent` on the two Confluent Cloud features | themathmagician | `make bdd TRANSPORT=kafka` leaves them out |

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
