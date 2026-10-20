# Release 0.0.6

**Version:** 0.0.6 | **Date:** 2026-10-20 | **Updated:** 2026-10-20 | **Branch:** `eventing-without-confluent`

[![cicd](https://github.com/Mathmagicians/confluent-eventing-playground/actions/workflows/cicd.yaml/badge.svg?branch=eventing-without-confluent)](https://github.com/Mathmagicians/confluent-eventing-playground/actions/workflows/cicd.yaml?query=branch%3Aeventing-without-confluent)

The spec's results: the badge's latest run, its job summary with the scenarios, and its `cucumber-report` artifact;
locally `build/reports/cucumber/index.html` in each project with features, after `make bdd`. The release ends In
production, [verification.md](04_verification.md).

## Catalog

| Idea | State | Map |
|---|---|---|
| Messaging capability: cli, kafka, confluent | Discovery | [discovery.md](01_discovery.md#messaging-capability-cli-kafka-confluent) |
| CI off Confluent | Discovery | [discovery.md](01_discovery.md#ci-off-confluent) |
| Stories describe themselves | Discovery | [discovery.md](01_discovery.md#stories-describe-themselves) |
| Architecture discovery: one model, three levels | Discovery | [discovery.md](01_discovery.md#architecture-discovery-one-model-three-levels) |
| The skill generate-platform-client | Discovery | [discovery.md](01_discovery.md#the-skill-generate-platform-client) |
| Slides alongside | Automation | [automation.md](03_automation.md#slides-alongside) |

## Rules

| Rule | Idea | Phase | Examples agreed | Spec signed | Spec green |
|---|---|---|---|---|---|
| on `cli` the console feeds the story and the log receives what it publishes | Messaging capability | Discovery | | | |
| on `kafka` a broker and a schema registry run on the machine | Messaging capability | Discovery | | | |
| on `confluent` the keys come from `.env.<ENV>.private`, and startup checks them | Messaging capability | Discovery | | | |
| CI plays the spec on `kafka`; the `@confluent` features run on `confluent` | CI off Confluent | Discovery | | | |
| a story names the payload types it publishes | Stories describe themselves | Discovery | | | |
| a story's port carries its name and its description | Stories describe themselves | Discovery | | | |
| each topic's key strategy carries a name | Stories describe themselves | Discovery | | | |
| `model.dsl` is generated from the code; `workspace.dsl` and `styles.dsl` by hand | Architecture discovery | Discovery | | | |
| level 1, the forest | Architecture discovery | Discovery | | | |
| level 2, the trees | Architecture discovery | Discovery | | | |
| level 3, the hexagon | Architecture discovery | Discovery | | | |
| `make arch-check` compares the generated files exactly | Architecture discovery | Discovery | | | |
| the skill's input is the architecture spec and the published platform | The skill generate-platform-client | Discovery | | | |
| the client is done when its `make check` is green | The skill generate-platform-client | Discovery | | | |
| the run works offline | The skill generate-platform-client | Discovery | | | |
