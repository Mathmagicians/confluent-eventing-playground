# Discovery

The example map of each feature in the [catalog](release-spec-0.0.6.md): its story, its rules with examples, its
questions, and the decisions it raises for [docs/adr.md](../../adr.md).

## Load on demand: the transport and its environment

**Story:** I play the load when I choose, on the transport I pick: in test from a branch, in prod from `main`.

Examples agreed 2026-10-20.

### Rule: load-run.yaml runs on dispatch, with the transport confluent, kafka, or cloudflare, and the load arguments
- a dispatch with `transport=kafka` and no arguments: `make docker-smoke`, the minimum.
- a dispatch with arguments: `make docker-run` with them.
- the hour turns: nothing runs.

### Rule: from main the load plays in <transport>-prod, from any other branch in <transport>-test
- a dispatch from `eventing-without-confluent` with `transport=kafka`: the job runs in `kafka-test`, with `ENV=test`.
- a dispatch from `main` with `transport=confluent`: the job runs in `confluent-prod`, with `ENV=prod`.
- `<transport>-prod` takes deployments from `main` only; `make gh-main-protection` sets each transport's two
  environments.
- the `kafka-*` and `cloudflare-*` environments start empty.

## One name: wonderland-eventing-playground

**Story:** the project carries one name, without a vendor: Confluent is one of the transports it serves.

Examples agreed 2026-10-21.

### Rule: every spelling of the project's name is wonderland-eventing-playground
- the repository `Mathmagicians/wonderland-eventing-playground`, its badges, the GitHub Packages Maven URL, and
  `rootProject.name`.
- the image `ghcr.io/mathmagicians/wonderland-eventing-playground` and the compose project.
- `spring.application.name`, on the wire as `ce_source`.
- the Java package `dk.mathmagicians.playground.confluent` stays.

## Project sync: the board follows the catalog

**Story:** I see each feature of a release on the GitHub project board, in its phase, with what the phase files say
about it, and `make gh-project-sync` after a signed gate moves its card.

Examples agreed 2026-10-21.

- The project, [Wonderland-eventing-playground-spec-driven](https://github.com/orgs/Mathmagicians/projects/5),
  number 5, id `PVT_kwDOAd2-Xs4Bn-nR`, belongs to the Mathmagicians organization. The organization's issue field
  `phase` has the options `discovery`, `formulation`, `automation`, `verification`, `parking`, and `archive`. The
  board has a swimlane per milestone, one milestone per release.
- GitHub links commits to issues by `#12` and closes them by keyword, and has no keywords that set a field.
- `GITHUB_TOKEN` writes issues and contents and leaves an organization's project alone; a push made with it starts
  no run. The project's auto-add workflow, filter `is:issue`, puts each issue of the repository on the board.

### Rule: each feature in a release's catalog has an issue, linked in its row
- a row with an empty Issue cell, `make gh-project-sync` on its branch: the sync creates the issue, titled as the
  feature, and writes `[#N](url)` into the cell.
- a row with its issue: the sync creates none.
- `main` receives every row linked, through the pull request.

- every row of every release plan has a card; a release is a swimlane, its milestone, and has no card of its own.
  Decided 2026-10-21.

### Rule: an issue's body is the feature's sections of the phase files
- the card of Load on demand: its sections of `01_discovery.md`, `02_formulation.md`, and `03_automation.md`, under
  Discovery, Formulation, and Automation, with the links absolute into the synced branch.
- One name has no section in `02_formulation.md`: its card has Discovery and Automation.
- the body edited on GitHub: the next sync writes it from the files.

### Rule: an issue's Phase is its row's state, its milestone the version in its overview's file name
- Spec green signed, the row from Automation to Verification, synced: the card moves to Verification.
- every overview under `docs/releases/` but `archived/`: `this-pr/` and each planned `release-<version>/`; a
  feature of `release-0.0.7/` lies in the swimlane 0.0.7.
- a version without its milestone: the sync creates the milestone, without a due date.
- a state matches its Phase option in any case, `Automation` the option `automation`; a state without an option
  stops the sync with the state's name.

**Questions**
1. The release-wide parts, Context, README, and `04_verification.md`: they stay in the repository; a card is a row.
   Decided 2026-10-21.

## CI plays each transport

**Goal:** a matrix build for each of the transports, confluent excluded for now, with a comment about pricing.

- `cd` and `cicd-client-example` play the features against Confluent Cloud, which answers the keys with
  `SaslAuthenticationException` since 2026-09-29, see 0.0.7, Messaging capability.
  `main` takes a merge with both green, admins included, [branch-protection.json](../../../.github/branch-protection.json).
- The suite's drivers start the image with the suite's profiles, `<transport>,<env>`, and the keys the environment
  has, [GeneratorContainer.java:78](../../../platform/src/testFixtures/java/dk/mathmagicians/playground/confluent/eventing/bdd/GeneratorContainer.java#L78)
  and [StoryContainer.java:62](../../../platform/src/testFixtures/java/dk/mathmagicians/playground/confluent/eventing/bdd/StoryContainer.java#L62).
  Under `cli` the image publishes to its log and its console feeds it.
- A job with a matrix reports a check per entry, `bdd (cli)`. The matrix is a `bdd` job of its own, so `ci` and
  `cd` keep the names branch protection requires, and protection stays as it is. Decided 2026-10-21.
- CI plays each transport in a matrix: confluent sits out for Confluent Cloud's pricing, kafka until its broker and
  schema registry run on the runner, and the five features move to cli. Decided 2026-10-21.
- What CI runs is driven by the Makefile: the transports CI plays are `CI_TRANSPORTS` in `platform.mk`, with the
  reason each other transport sits out, and the matrix reads them from `make ci-transports`. Decided 2026-10-21.

**Story:** CI plays the spec on each transport we run, and a transport that sits out says why.

Examples agreed 2026-10-22.

### Rule: the bdd job plays a matrix of the transports; a transport that sits out carries its reason
- a pull request: `bdd (cli)` plays, then `cd` publishes the image, and `ci`, `cd`, and `cicd-client-example` are
  green.
- `make ci-transports`: `["cli"]`.
- confluent sits out: the comment at `CI_TRANSPORTS` names Confluent Cloud's pricing.
- kafka sits out: the same comment names the broker and the schema registry on the runner.

### Rule: each transport plays the scenarios it carries
- `make bdd`: plays as `TRANSPORT=cli`.
- `make bdd TRANSPORT=cli`: every scenario but `@kafka`, `@confluent`, `@wip`, and `@manual`.
- `make bdd TRANSPORT=kafka`: every scenario but `@confluent`, `@wip`, and `@manual`.
- `make bdd TRANSPORT=confluent`: every scenario but `@wip` and `@manual`.
- the Background `Given I can connect to the eventing system`: in every feature, checked on the transport played.

### Rule: on cli the console feeds the story and the log receives what it publishes
- the generator's feature on cli: the envelope's id and region in the image's log.
- the tea party and the purse on cli: the offers and the orders typed on the console, the transactions in the log.
- cli plays the image's `cli` profile, see 0.0.7, Messaging capability.

**Questions**
1. The default of `TRANSPORT`: `cli`, which plays without a broker and without keys. Decided 2026-10-21.

**Decisions for docs/adr.md, by title**
- [ADR-001 CI plays each transport in a matrix](../../adr.md#adr-001-ci-plays-each-transport-in-a-matrix)
