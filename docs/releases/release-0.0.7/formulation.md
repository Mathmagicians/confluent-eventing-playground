# Formulation

Each feature's scenarios and architecture rules, by link, and the step skeletons, for the features of the
[catalog](release-0.0.7.md) that passed Examples agreed. The evidence of Spec signed, the red run, is linked from the
feature's row in the catalog.

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
