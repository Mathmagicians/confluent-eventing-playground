# Release

**Date:** 2026-10-20 | **Updated:** 2026-10-21 | **Branch:** `eventing-without-confluent`

[![cicd](https://github.com/Mathmagicians/wonderland-eventing-playground/actions/workflows/cicd.yaml/badge.svg?branch=eventing-without-confluent)](https://github.com/Mathmagicians/wonderland-eventing-playground/actions/workflows/cicd.yaml?query=branch%3Aeventing-without-confluent)

The plumbing, shipped as soon as it is ready. The spec's results: the badge's latest run, its job summary with the
scenarios, and its `cucumber-report` artifact; locally `build/reports/cucumber/index.html` in each project with
features, after `make bdd`. The release ends In production, [verification.md](04_verification.md).

## Catalog

| Feature | Issue | State | Examples agreed | Spec signed | Spec green | Map |
|---|---|---|---|---|---|---|
| Load on demand: the transport and its environment | [#13](https://github.com/Mathmagicians/wonderland-eventing-playground/issues/13) | Automation | [2026-10-20](01_discovery.md#load-on-demand-the-transport-and-its-environment) | [2026-10-21](02_formulation.md#load-on-demand-the-transport-and-its-environment) | | [discovery.md](01_discovery.md#load-on-demand-the-transport-and-its-environment) |
| Slides spike: the talk plays from compose | [#14](https://github.com/Mathmagicians/wonderland-eventing-playground/issues/14) | Verification | | | [2026-10-20](03_automation.md#slides-spike-the-talk-plays-from-compose) | [automation.md](03_automation.md#slides-spike-the-talk-plays-from-compose) |
| One name: wonderland-eventing-playground | [#15](https://github.com/Mathmagicians/wonderland-eventing-playground/issues/15) | Verification | [2026-10-21](01_discovery.md#one-name-wonderland-eventing-playground) | | [2026-10-21](03_automation.md#one-name-wonderland-eventing-playground) | [discovery.md](01_discovery.md#one-name-wonderland-eventing-playground) |
| Project sync: the board follows the catalog | [#16](https://github.com/Mathmagicians/wonderland-eventing-playground/issues/16) | Automation | [2026-10-21](01_discovery.md#project-sync-the-board-follows-the-catalog) | [2026-10-21](02_formulation.md#project-sync-the-board-follows-the-catalog) | | [discovery.md](01_discovery.md#project-sync-the-board-follows-the-catalog) |
| The catalog follows the board | [#17](https://github.com/Mathmagicians/wonderland-eventing-playground/issues/17) | Discovery | | | | [discovery.md](01_discovery.md#the-catalog-follows-the-board) |
| A release ships what is ready | [#18](https://github.com/Mathmagicians/wonderland-eventing-playground/issues/18) | Discovery | | | | [discovery.md](01_discovery.md#a-release-ships-what-is-ready) |
| Messaging capability: cli, kafka, confluent | [#7](https://github.com/Mathmagicians/wonderland-eventing-playground/issues/7) | Discovery | | | | [discovery.md](01_discovery.md#messaging-capability-cli-kafka-confluent) |
| CI plays each transport | [#19](https://github.com/Mathmagicians/wonderland-eventing-playground/issues/19) | Discovery | | | | [discovery.md](01_discovery.md#ci-plays-each-transport) |
