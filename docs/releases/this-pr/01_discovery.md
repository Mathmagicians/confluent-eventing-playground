# Discovery · 0.0.6

The example map of each idea in the [catalog](release-spec-0.0.6.md): its story, its rules with examples, its questions,
and the decisions it raises for [docs/adr.md](../../adr.md).

## Context

- Confluent Cloud answers the keys of `confluent-test` and `confluent-prod` with `SaslAuthenticationException:
  Authentication failed`, in every `load-run` since 2026-09-29 12:08 UTC. The cluster stays out of service.
- `cd` and `cicd-client-example` run the features against that cluster.
- A process has one switch, its profile: `local` publishes to the log, `test` and `prod` to Confluent Cloud through
  `kafka.properties`, imported at [application-test.properties:2](../../../platform/src/main/resources/application-test.properties#L2).

## Messaging capability: cli, kafka, confluent

**Goal:** all three start locally with easy make targets: the log works, `kafka` starts a local container, and
`confluent` fails gracefully about its credentials.

**Story:** I pick where a story plays, the log, a local broker, or Confluent Cloud, with one make variable.

### Rule: on cli the console feeds the story and the log receives what it publishes
- `make run-tiny TRANSPORT=cli`: a line `Published <id> key <key>: <payload>` at INFO, then the process ends.

### Rule: on kafka a broker and a schema registry run on the machine, with the topics and schemas iac/ gives Confluent Cloud
- `make run-tiny` with nothing running: the broker and the registry start, the schemas register, and the generator's
  message lands on `test.offers`.
- `make run-tiny` a second time: the broker is up already, and the offsets continue from the first run.
- `make kafka-down`: the broker and the registry stop, and what they held is gone.

### Rule: on confluent the keys come from .env.ENV.private, and startup checks them
- `make run-tiny TRANSPORT=confluent` without `.env.test.private`: `APPLICATION FAILED TO START`, naming each
  missing variable, exit code 1.
- with `KAFKA_API_SECRET` alone missing: the message names `KAFKA_API_SECRET`.
- with the keys `.env.test.private` holds, which the cluster rejects: see question 2.

**Questions**
1. A process that names an environment alone, `SPRING_PROFILES_ACTIVE=test`, as the drivers and the workflows set
   it: Spring's own failure for the missing `Publisher` bean, or a message naming `cli`, `kafka`, and `confluent`?
2. The keys the cluster rejects: startup fails on the first authentication error, or the story runs and logs each
   failed send at ERROR, as [KafkaPublisher.java:47](../../../platform/src/main/java/dk/mathmagicians/playground/confluent/eventing/adapter/kafka/publisher/KafkaPublisher.java#L47) does?
3. The Background `Given I have the API keys to the cluster`, in all five features
   ([ClusterSteps.java:15](../../../platform/src/testFixtures/java/dk/mathmagicians/playground/confluent/eventing/bdd/ClusterSteps.java#L15)):
   reworded for every transport, or kept on the `@confluent` features?
4. The schema registry image: `confluentinc/cp-schema-registry` (Confluent, Confluent Community License), or
   Karapace, `ghcr.io/aiven-open/karapace` (Aiven, Apache 2.0)?
5. `make run-tiny`, the quick start of the Definition of done: on `cli`, or on `TRANSPORT`?
6. The default of `TRANSPORT`: `kafka`, the loop CI runs, or `cli`, which runs without Docker?

**Decisions for docs/adr.md, by title**
- The transport is a profile beside the environment

## CI off Confluent

**Story:** CI plays the spec, and the load plays on demand, with Confluent Cloud out of service.

### Rule: CI plays the spec on kafka; the features about Confluent Cloud carry @confluent and run on confluent
- a pull request: `cd` and `cicd-client-example` run the features on a broker on the runner, green.
- [the_cluster_has_my_topics_and_schemas.feature](../../../platform/src/test/resources/features/the_cluster_has_my_topics_and_schemas.feature)
  and [the_flink_cluster_in_the_cloud_keeps_the_latest_value_snapshot.feature](../../../platform/src/test/resources/features/the_flink_cluster_in_the_cloud_keeps_the_latest_value_snapshot.feature)
  carry `@confluent`.
- `load-run.yaml` runs on `workflow_dispatch`, with the transport, `confluent`, `kafka`, or `cloudflare`, and the
  load arguments as inputs; it plays in the GitHub environment `<transport>-prod` from `main` and in
  `<transport>-test` from any other branch, and the `kafka-*` and `cloudflare-*` environments start empty. Set by
  themathmagician 2026-10-20.

**Questions**
1. Production is the environments Wonderland plays in, `test` and `prod`: which transport each plays on, with
   Confluent Cloud out of service?
2. The purse in `cicd-client-example`: the platform's `kafka-up` through `platform.mk`, or broker services in its
   own [compose.yaml](../../../examples/purse/compose.yaml)?

**Decisions for docs/adr.md, by title**
- CI plays the spec on a local broker

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
