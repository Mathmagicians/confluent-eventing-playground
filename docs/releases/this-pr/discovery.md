# Discovery

The example map of each feature in the [catalog](release-0.0.7.md): its story, its rules with examples, its
questions, and the decisions it raises for [docs/adr.md](../../adr.md).

## Messaging capability: cli, kafka, confluent

**Goal:** all three start locally with easy make targets: the log works, `kafka` starts a local container, and
`confluent` fails gracefully about its credentials.

- Confluent Cloud answers the keys of `confluent-test` and `confluent-prod` with `SaslAuthenticationException:
  Authentication failed`, in every `load-run` since 2026-09-29 12:08 UTC. The cluster stays out of service.
- `cli` publishes to the log, `confluent` to Confluent Cloud through `kafka.properties`, imported at
  [application-confluent.properties:2](../../../platform/src/main/resources/application-confluent.properties#L2);
  the Kafka adapters carry `@Profile("confluent")`, and `kafka` has no profile yet.
- 0.0.6 ships the transports `cli` and `confluent`, with
  [CI plays each transport](../archived/release-0.0.6/01_discovery.md#ci-plays-each-transport), its matrix played on `cli`; 0.0.7
  adds `kafka`. Decided 2026-10-22.
- A transport is a profile, `cli`, `kafka`, `confluent`; an environment is the GitHub environment a run plays
  against or deploys to, `test` or `prod`, and a profile of its own naming the topics. A process runs
  `<transport>,<env>`. Decided 2026-10-21.

**Story:** I pick where a story plays, the log, a local broker, or Confluent Cloud, with one make variable.

### Rule: on cli the console feeds the story and the log receives what it publishes
- `make run-tiny`: a line `Published <id> to log-0 at offset <offset>, key <key>, region <region>: <payload>` at
  INFO, the payload as the console reads it, then the process ends.

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
3. The Background `Given I have the API keys to the cluster`, in all five features: kept in all five as
   `Given I can connect to the eventing system`,
   [TransportSteps.java:14](../../../platform/src/testFixtures/java/dk/mathmagicians/playground/confluent/eventing/bdd/TransportSteps.java#L14);
   the step asks the transport played: on `confluent` the keys open the cluster, on `cli` the Docker host answers.
   Decided 2026-10-22.
4. Production is the environments Wonderland plays in, `test` and `prod`: which transport each plays on, with
   Confluent Cloud out of service?
5. The purse on `kafka`: the platform's `kafka-up` through `platform.mk`, or broker services in its own
   [compose.yaml](../../../examples/purse/compose.yaml)?
6. The schema registry image: `confluentinc/cp-schema-registry` (Confluent, Confluent Community License), or
   Karapace, `ghcr.io/aiven-open/karapace` (Aiven, Apache 2.0)?
7. `make run-tiny`, the quick start of the Definition of done: on `TRANSPORT`, default `cli`. Decided 2026-10-21.

**Decisions for docs/adr.md, by title**
- The transport is a profile beside the environment

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
- a Phase written by `make gh-project-sync`: the run finds the row in that state and commits nothing.

### Rule: a release's milestone names its branch
- `make gh-project-sync` on `eventing-without-confluent`: the milestone 0.0.6 has the description
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
4. A shipped feature's issue is closed by `closes #N` in a commit, when the commit reaches `main`; the pull moves its
   card to `archive`. Decided 2026-10-22.

**Decisions for docs/adr.md, by title**
- The board writes a feature's phase to the catalog through the issue field Phase

## A release ships what is ready

**Goal:** a release ships the subset that is ready.

- `make bdd` plays every feature file: the `bdd` task at [platform.gradle:103](../../../platform.gradle#L103) and
  [junit-platform.properties](../../../platform/src/testFixtures/resources/junit-platform.properties) set no tag
  filter. A pending step is a red run, the evidence of Spec signed, and a red feature blocks the build.
- A card's milestone is its overview's version, see
  [Project sync](../archived/release-0.0.6/01_discovery.md#project-sync-the-board-follows-the-catalog).
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

## Stories describe themselves

**Story:** as the platform, I read from each story what it hears, what it publishes, and on which key.

- [Story.java](../../../platform/src/main/java/dk/mathmagicians/playground/confluent/eventing/application/Story.java)
  names what a story reads, `listensTo()` and `hears()`; what it publishes lives in its body.
- jMolecules 2.0.1's `@PrimaryPort` carries `name` and `description`.
- The key per topic is the README's rule under Purpose, and `Envelope.key()` in the code.

### Rule: a story names the payload types it publishes, Story.publishes(), beside listensTo()
### Rule: a story's port carries its name and its description, @PrimaryPort(name, description)
### Rule: each topic's key strategy carries a name: by product, by region and product, by region, by customer

**Questions**
1. The home of a key strategy's name: the payload record, or `Topics`?

## Architecture discovery: one model, three levels

**Story:** as a reader of the architecture, I see the platform at three levels, drawn from one model the code
writes.

- `HexagonDocumenter` in [architecture-discovery/](../../../architecture-discovery/architecture-discoverability.md)
  writes PlantUML under `docs/generated/architecture` from the annotations; `make arch-check` compares its sources
  line-sorted, [platform.mk:107](../../../platform.mk#L107).
- structurizr-core and structurizr-export 5.0.3 come on the classpath with Spring Modulith.

### Rule: model.dsl is generated from the code through ModuleDocumentation; workspace.dsl and styles.dsl are written by hand
### Rule: level 1, the forest: the landscape, the platform with its stories and the purse
### Rule: level 2, the trees: the containers, with their connections
### Rule: level 3, the hexagon of each container: the rings and the adapters
### Rule: make arch-check compares the generated files exactly

**Questions**
1. The rendering: structurizr-dsl 6.2.3 in the build, or the Structurizr CLI image? Both external, a go on the name.

## The skill generate-platform-client

**Story:** from the architecture spec, the skill builds a client of the platform that passes it.

- The purse extends `HexagonalArchitecture` and `ModuleDocumentation` with one-line classes, and 7 of the 13 step
  lines of its feature are platform steps.
- `make platform-install` puts the platform, its test fixtures, and the stories in the local Maven repository,
  [Makefile:116](../../../Makefile#L116).

### Rule: the skill's input is the architecture rules, the shared steps, model.dsl, and the published platform
### Rule: the client is done when its make check is green: the rules, its features, the platform's steps
### Rule: the run works offline, from make platform-install

**Questions**
1. The client for the demo: a second purse, or a new story?
