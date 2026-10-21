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
about it, and the commit that records a gate moves its card.

Examples agreed 2026-10-21.

- The project, [Wonderland-eventing-playground-spec-driven](https://github.com/orgs/Mathmagicians/projects/5),
  number 5, id `PVT_kwDOAd2-Xs4Bn-nR`, belongs to the Mathmagicians organization. The organization's issue field
  `phase` has the options `discovery`, `formulation`, `automation`, `verification`, `parking`, and `archive`. The
  board has a swimlane per milestone, one milestone per release.
- GitHub links commits to issues by `#12` and closes them by keyword, and has no keywords that set a field.
- `GITHUB_TOKEN` writes issues and contents and leaves an organization's project alone; a push made with it starts
  no run. The project's auto-add workflow, filter `is:issue`, puts each issue of the repository on the board.

### Rule: each feature in a release's catalog has an issue, linked in its row
- a row with an empty Issue cell, pushed on a branch: the sync creates the issue, titled as the feature, and a second
  step pushes the catalog with `[#N](url)` in the cell.
- a row with its issue: the sync creates none.
- `main` receives every row linked, through the pull request; a push of links to `main` is refused.

- every row of every release plan has a card; a release is a swimlane, its milestone, and has no card of its own.
  Decided 2026-10-21.

### Rule: an issue's body is the feature's sections of the phase files
- the card of Load on demand: its sections of `01_discovery.md`, `02_formulation.md`, and `03_automation.md`, under
  Discovery, Formulation, and Automation, with the links absolute into the pushed branch.
- One name has no section in `02_formulation.md`: its card has Discovery and Automation.
- the body edited on GitHub: the next sync writes it from the files.

### Rule: an issue's Phase is its row's state, its milestone the version in its overview's file name
- Spec green signed, the row from Automation to Verification, pushed: the card moves to Verification.
- every overview under `docs/releases/` but `archived/`: `this-pr/` and each planned `release-<version>/`; a
  feature of `release-0.0.7/` lies in the swimlane 0.0.7.
- a version without its milestone: the sync creates the milestone, without a due date.
- a state matches its Phase option in any case, `Automation` the option `automation`; a state without an option
  stops the sync with the state's name.

**Questions**
1. The release-wide parts, Context, README, and `04_verification.md`: they stay in the repository; a card is a row.
   Decided 2026-10-21.

## The catalog follows the board

**Goal:** a project update is a release doc update; a change on the board is for members only.

- Actions has no trigger for a project's fields. The `projects_v2_item` webhook is an organization's, in public
  preview, [webhook events](https://docs.github.com/en/webhooks/webhook-events-and-payloads#projects_v2_item).
- An issue field is an organization's single select on the issue. Setting it fires the `issues` event, `field_added`,
  with the field's name, its value, and its previous value, and a board groups its columns by it,
  [about issue fields](https://docs.github.com/en/issues/planning-and-tracking-with-projects/understanding-fields/about-issue-fields).
  Mathmagicians has issue fields: Effort, Priority, Start date, and Target date, each Organization only.
- An issue field is Organization only by default, visible to the organization's members and the repository's
  collaborators,
  [managing issue fields](https://docs.github.com/en/issues/tracking-your-work-with-issues/using-issues/managing-issue-fields-in-your-organization).
  Setting a value takes push access to the repository,
  [issue field values](https://docs.github.com/en/rest/issues/issue-field-values).
- Project 5 is private: those granted read access see it, and the base role Write lets the organization's members
  edit it,
  [managing access](https://docs.github.com/en/issues/planning-and-tracking-with-projects/managing-your-project/managing-access-to-your-projects).
- The `issues` event runs the workflow on `main`'s last commit,
  [events that trigger workflows](https://docs.github.com/en/actions/reference/workflows-and-actions/events-that-trigger-workflows#issues),
  and `main` holds no `docs/releases`.

**Story:** as a member of Mathmagicians, I move a card on the board, and the release plan on the release's branch
says so.

### Rule: a feature's phase is the issue field Phase; the board's columns are its options
- the options: `discovery`, `formulation`, `automation`, `verification`, `parking`, `archive`.

### Rule: a member of Mathmagicians moves the board's cards
- themathmagician sets a card's Phase: the release plan follows, see the next rule.
- the board opened signed out: it is not shown.
- the project is private, its base role Write, and Phase is Organization only.

### Rule: a Phase set on the board is its row's state and its gate's date, on the release's branch
- the card of Project sync set to `verification`: `project.yaml` runs on `field_added`, and
  `eventing-without-confluent` receives the commit `docs: Project sync: the board follows the catalog to
  verification`, the row reading Verification with 2026-10-21 in its Spec green cell, linked to the feature's
  section of `03_automation.md`, where our loop patches the evidence in.
- the card of One name set to `archive`: the row reads Archive; In production is the release's gate, signed in
  `04_verification.md`.
- that commit's push starts no run, and the issue's body says the new state.
- a Phase written by `make project-sync`: the run finds the row in that state and commits nothing.

### Rule: a release's milestone names its branch
- `make project-sync` on `eventing-without-confluent`: the milestone 0.0.6 has the description
  `eventing-without-confluent`, the overview's Branch.
- a card of 0.0.7 moved, its overview naming no branch: its Phase goes back to the row's state, and the job summary
  names the card.

**Questions**
1. The gate a move signs, Examples agreed on the move to `formulation`, Spec signed to `automation`, Spec green to
   `verification`: the pull writes its date into the feature's row, and our loop patches the evidence into the
   feature's section of the phase file the card left. In production is the release's gate. A workflow that verifies
   a gate, as `cicd.yaml` verifies a release, is a feature of its own. Decided 2026-10-21.
2. A row is a feature, and a move changes its row; a release groups the features it releases. A feature is Spec green
   on its passing BDD tests, or on a manual verification where BDD tests are not feasible. Decided 2026-10-21.
3. Phase in place of Status, one field on the board: Project sync writes Phase, and the project's Status stays
   unused. Decided 2026-10-21.

**Decisions for docs/adr.md, by title**
- The board writes a feature's phase to the catalog through the issue field Phase

## A release ships what is ready

**Goal:** a release ships the subset that is ready.

- `make bdd` plays every feature file: the `bdd` task at [platform.gradle:103](../../../platform.gradle#L103) and
  [junit-platform.properties](../../../platform/src/testFixtures/resources/junit-platform.properties) set no tag
  filter. A pending step is a red run, the evidence of Spec signed, and a red feature blocks the build.
- A card's milestone is its overview's version, see
  [Project sync](#project-sync-the-board-follows-the-catalog).
- A feature we decide is not ready carries `@wip`, and a check played by hand, written as a scenario, carries
  `@manual`: `make bdd` leaves both out, and `make bdd-snippets` leaves out `@manual` alone. Decided 2026-10-21.
- The repository's own features, the tooling's, live in `src/test/resources/features/` at the root, played by a
  `bdd` of the root project. Decided 2026-10-21.

**Story:** I release the features that are ready; the others move on to the next release.

### Rule: a release ships its rows in Verification; the others move to the next release's catalog
- the merge of 0.0.6: its rows in Verification read Archive, signed In production in `04_verification.md`.
- a feature in Discovery at the merge: its row moves to the catalog of 0.0.7, and its card to the swimlane 0.0.7.

### Rule: a feature we decide is not ready carries @wip, and make bdd leaves it out
- `make bdd`: every scenario but those tagged `@wip` or `@manual`.
- a feature decided not ready for 0.0.6: its feature file carries `@wip`, and the release ships green without it.
- `make bdd-snippets`: the snippets for the undefined steps of every scenario, `@wip` included.

### Rule: a check played by hand carries @manual, and its evidence is in the phase file
- the talk of the Slides spike as a scenario tagged `@manual`: `make bdd` and `make bdd-snippets` leave it out.
- its Spec green in `03_automation.md`: what themathmagician saw, with the date.

**Decisions for docs/adr.md, by title**
- A release ships the features that are Spec green

## Messaging capability: cli, kafka, confluent

**Goal:** all three start locally with easy make targets: the log works, `kafka` starts a local container, and
`confluent` fails gracefully about its credentials.

- Confluent Cloud answers the keys of `confluent-test` and `confluent-prod` with `SaslAuthenticationException:
  Authentication failed`, in every `load-run` since 2026-09-29 12:08 UTC. The cluster stays out of service.
- A process has one switch, its profile: `local` publishes to the log, `test` and `prod` to Confluent Cloud through
  `kafka.properties`, imported at [application-test.properties:2](../../../platform/src/main/resources/application-test.properties#L2).
- Messaging capability is the goal of this branch, eventing without Confluent, and ships in 0.0.6. Decided
  2026-10-21.
- A transport is a profile, `cli`, `kafka`, `confluent`; an environment is the GitHub environment a run plays
  against or deploys to, `test` or `prod`, and a profile of its own naming the topics. A process runs
  `<transport>,<env>`. Decided 2026-10-21.

**Story:** I pick where a story plays, the log, a local broker, or Confluent Cloud, with one make variable.

### Rule: on cli the console feeds the story and the log receives what it publishes
- `make run-tiny`: a line `Published <id> key <key>: <payload>` at INFO, then the process ends.

### Rule: on kafka a broker and a schema registry run on the machine, with the topics and schemas iac/ gives Confluent Cloud
- `make run-tiny TRANSPORT=kafka` with nothing running: the broker and the registry start, the schemas register,
  and the generator's message lands on `test.offers`.
- `make run-tiny TRANSPORT=kafka` a second time: the broker is up already, and the offsets continue from the first
  run.
- `make kafka-down`: the broker and the registry stop, and what they held is gone.

### Rule: on confluent the keys come from .env.ENV.private, and startup checks them
- `make run-tiny TRANSPORT=confluent` without `.env.test.private`: `APPLICATION FAILED TO START`, naming each
  missing variable, exit code 1.
- with `KAFKA_API_SECRET` alone missing: the message names `KAFKA_API_SECRET`.
- with the keys `.env.test.private` holds, which the cluster rejects: see question 2.

**Questions**
1. A process that names an environment alone, `SPRING_PROFILES_ACTIVE=test`: Spring's own failure for the missing
   `Publisher` bean, or a message naming `cli`, `kafka`, and `confluent`?
2. The keys the cluster rejects: startup fails on the first authentication error, or the story runs and logs each
   failed send at ERROR, as [KafkaPublisher.java:47](../../../platform/src/main/java/dk/mathmagicians/playground/confluent/eventing/adapter/kafka/publisher/KafkaPublisher.java#L47) does?
3. The Background `Given I have the API keys to the cluster`, in all five features
   ([ClusterSteps.java:15](../../../platform/src/testFixtures/java/dk/mathmagicians/playground/confluent/eventing/bdd/ClusterSteps.java#L15)):
   kept on the `@confluent` features, see [CI plays each transport](#ci-plays-each-transport). Decided 2026-10-21.
4. Production is the environments Wonderland plays in, `test` and `prod`: which transport each plays on, with
   Confluent Cloud out of service?
5. The purse on `kafka`: the platform's `kafka-up` through `platform.mk`, or broker services in its own
   [compose.yaml](../../../examples/purse/compose.yaml)?
6. The schema registry image: `confluentinc/cp-schema-registry` (Confluent, Confluent Community License), or
   Karapace, `ghcr.io/aiven-open/karapace` (Aiven, Apache 2.0)?
7. `make run-tiny`, the quick start of the Definition of done: on `TRANSPORT`, default `cli`. Decided 2026-10-21.

**Decisions for docs/adr.md, by title**
- The transport is a profile beside the environment

## CI plays each transport

**Goal:** a matrix build for each of the transports, confluent excluded for now, with a comment about pricing.

- `cd` and `cicd-client-example` play the features against Confluent Cloud, which answers the keys with
  `SaslAuthenticationException` since 2026-09-29, see [Messaging capability](#messaging-capability-cli-kafka-confluent).
  `main` takes a merge with both green, admins included, [branch-protection.json](../../../.github/branch-protection.json).
- The suite's drivers start the image with `SPRING_PROFILES_ACTIVE=test` and the keys,
  [GeneratorContainer.java:70](../../../platform/src/testFixtures/java/dk/mathmagicians/playground/confluent/eventing/bdd/GeneratorContainer.java#L70)
  and [StoryContainer.java:71](../../../platform/src/testFixtures/java/dk/mathmagicians/playground/confluent/eventing/bdd/StoryContainer.java#L71).
  Under `local` the image publishes to its log and its console feeds it.
- A job with a matrix reports a check per entry, `bdd (cli)`. The matrix is a `bdd` job of its own, so `ci` and
  `cd` keep the names branch protection requires, and protection stays as it is. Decided 2026-10-21.
- CI plays each transport in a matrix: confluent sits out for Confluent Cloud's pricing, kafka until its broker and
  schema registry run on the runner, and the five features move to cli. Decided 2026-10-21.
- What CI runs is driven by the Makefile: the transports CI plays are `CI_TRANSPORTS` in `platform.mk`, with the
  reason each other transport sits out, and the matrix reads them from `make ci-transports`. Decided 2026-10-21.

**Story:** CI plays the spec on each transport we run, and a transport that sits out says why.

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
- the Background `Given I have the API keys to the cluster`: on the `@confluent` features alone.

### Rule: on cli the console feeds the story and the log receives what it publishes
- the generator's feature on cli: the envelope's id and region in the image's log.
- the tea party and the purse on cli: the offers and the orders typed on the console, the transactions in the log.
- cli plays the image's `cli` profile, see [Messaging capability](#messaging-capability-cli-kafka-confluent).

**Questions**
1. The default of `TRANSPORT`: `cli`, which plays without a broker and without keys. Decided 2026-10-21.

**Decisions for docs/adr.md, by title**
- CI plays each transport in a matrix
