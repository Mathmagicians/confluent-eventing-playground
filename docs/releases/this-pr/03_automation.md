# Automation · 0.0.6

The tasks of each idea in the [catalog](release-spec-0.0.6.md), each with its owner and the scenario it turns green, and
the core snippets. One go per change. The evidence of Spec green, the green run and the ADRs a rule touches, is
linked from the rule's row in the catalog.

## Messaging capability: cli, kafka, confluent

### Profiles: cli, kafka, confluent

Owner: pair for the properties, themathmagician for the annotations. Turns green: every scenario of the map.

[platform.properties](../../../platform/src/main/resources/platform.properties), shared by every distribution, takes
the default from [app/.../application.properties:6](../../../app/src/main/resources/application.properties#L6) and
[examples/purse/.../application.properties:6](../../../examples/purse/src/main/resources/application.properties#L6):

```properties
# the transport when none is given: the console and the log
spring.profiles.default=cli
# Confluent Cloud is Kafka with credentials
spring.profiles.group.confluent=kafka
```

`platform/src/main/resources/application-kafka.properties` is
[kafka.properties](../../../platform/src/main/resources/kafka.properties) with the local defaults, lines 7 to 39 as
they are:

```properties
# the broker and the registry of make kafka-up, unless the environment names others
spring.kafka.bootstrap-servers=${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
spring.kafka.properties.schema.registry.url=${SCHEMA_REGISTRY_REST_ENDPOINT:http://localhost:8081}
```

`platform/src/main/resources/application-confluent.properties` holds what Confluent Cloud adds, and the values
`ConfluentKeys` checks:

```properties
# Confluent Cloud: SASL_SSL with PLAIN, the cluster API key as the credentials, the registry key as basic auth
spring.kafka.security.protocol=SASL_SSL
spring.kafka.properties.sasl.mechanism=PLAIN
spring.kafka.properties.sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required username="${KAFKA_API_KEY:}" password="${KAFKA_API_SECRET:}";
spring.kafka.properties.basic.auth.credentials.source=USER_INFO
spring.kafka.properties.basic.auth.user.info=${SCHEMA_REGISTRY_API_KEY:}:${SCHEMA_REGISTRY_API_SECRET:}

# what startup checks, by the names of .env.<ENV>.private
confluent.bootstrap-servers=${KAFKA_BOOTSTRAP_SERVERS:}
confluent.api-key=${KAFKA_API_KEY:}
confluent.api-secret=${KAFKA_API_SECRET:}
confluent.registry-endpoint=${SCHEMA_REGISTRY_REST_ENDPOINT:}
confluent.registry-api-key=${SCHEMA_REGISTRY_API_KEY:}
confluent.registry-api-secret=${SCHEMA_REGISTRY_API_SECRET:}
```

`application-test.properties` and `application-prod.properties` hold the topic names.

The annotations:

| File | Current | Replacement |
|---|---|---|
| [LoggingPublisher.java:18](../../../platform/src/main/java/dk/mathmagicians/playground/confluent/eventing/adapter/cli/log/LoggingPublisher.java#L18), [ConsoleListener.java:44](../../../platform/src/main/java/dk/mathmagicians/playground/confluent/eventing/adapter/cli/console/ConsoleListener.java#L44) | `@Profile("local")` | `@Profile("cli")` |
| [KafkaPublisher.java:26](../../../platform/src/main/java/dk/mathmagicians/playground/confluent/eventing/adapter/kafka/publisher/KafkaPublisher.java#L26), [StoryListener.java:26](../../../platform/src/main/java/dk/mathmagicians/playground/confluent/eventing/adapter/kafka/consumer/StoryListener.java#L26), [Reader.java:18](../../../platform/src/main/java/dk/mathmagicians/playground/confluent/eventing/adapter/kafka/consumer/Reader.java#L18), [DeadLetters.java:23](../../../platform/src/main/java/dk/mathmagicians/playground/confluent/eventing/adapter/kafka/consumer/DeadLetters.java#L23), [Topics.java:14](../../../platform/src/main/java/dk/mathmagicians/playground/confluent/eventing/adapter/kafka/Topics.java#L14) | `@Profile("!local")` | `@Profile("kafka")` |

### ConfluentKeys, checked at startup

Owner: themathmagician, with `ConfluentKeysTest`. Turns green: on `confluent` without keys, startup names each
missing variable.

`platform/src/main/java/.../eventing/adapter/kafka/ConfluentKeys.java`, bound by the `@ConfigurationPropertiesScan`
at [EventingPlatform.java:12](../../../platform/src/main/java/dk/mathmagicians/playground/confluent/EventingPlatform.java#L12)
as `Topics` is. Spring Boot reports the failed binding as `APPLICATION FAILED TO START` with the message:

```java
/// The Confluent Cloud keys, by the names of `.env.<ENV>.private` and the GitHub environments. Startup fails naming
/// every variable that is missing.
@ConfigurationProperties("confluent")
@Profile("confluent")
public record ConfluentKeys(String bootstrapServers, String apiKey, String apiSecret,
                            String registryEndpoint, String registryApiKey, String registryApiSecret) {

    public ConfluentKeys {
        var missing = Stream.of(
                        new Variable("KAFKA_BOOTSTRAP_SERVERS", bootstrapServers),
                        new Variable("KAFKA_API_KEY", apiKey),
                        new Variable("KAFKA_API_SECRET", apiSecret),
                        new Variable("SCHEMA_REGISTRY_REST_ENDPOINT", registryEndpoint),
                        new Variable("SCHEMA_REGISTRY_API_KEY", registryApiKey),
                        new Variable("SCHEMA_REGISTRY_API_SECRET", registryApiSecret))
                .filter(Variable::isMissing)
                .map(Variable::name)
                .toList();
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException("Confluent Cloud needs " + String.join(", ", missing));
        }
    }

    private record Variable(String name, @Nullable String value) {
        boolean isMissing() {
            return value == null || value.isBlank();
        }
    }
}
```

### The local broker and registry

Owner: pair. Turns green: the `kafka` examples. The images enter on a go on their names: `apache/kafka:4.2.1`
(Apache Software Foundation, Apache 2.0), the version of the `kafka-clients` the Boot BOM brings, and the registry
of question 4.

[compose.yaml](../../../compose.yaml) gains a `kafka` compose profile. The broker creates a topic on first use with
the partition count of [iac/locals.tf:8](../../../iac/locals.tf#L8), as the README's test containers do:

```yaml
  # the local broker, make kafka-up: localhost:9092 for the host, kafka:19092 for the flock
  kafka:
    image: apache/kafka:4.2.1
    profiles: [kafka]
    ports: ["9092:9092"]
    environment:
      KAFKA_NODE_ID: 1
      KAFKA_PROCESS_ROLES: broker,controller
      KAFKA_LISTENERS: HOST://:9092,FLOCK://:19092,CONTROLLER://:9093
      KAFKA_ADVERTISED_LISTENERS: HOST://localhost:9092,FLOCK://kafka:19092
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: HOST:PLAINTEXT,FLOCK:PLAINTEXT,CONTROLLER:PLAINTEXT
      KAFKA_INTER_BROKER_LISTENER_NAME: FLOCK
      KAFKA_CONTROLLER_LISTENER_NAMES: CONTROLLER
      KAFKA_CONTROLLER_QUORUM_VOTERS: 1@kafka:9093
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_NUM_PARTITIONS: 6   # iac/locals.tf partitions

  # the local registry, make kafka-up; make kafka-init registers the schemas
  registry:
    image: confluentinc/cp-schema-registry:8.3.1   # question 4
    profiles: [kafka]
    depends_on: [kafka]
    ports: ["8081:8081"]
    environment:
      SCHEMA_REGISTRY_HOST_NAME: registry
      SCHEMA_REGISTRY_LISTENERS: http://0.0.0.0:8081
      SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS: kafka:19092
```

The `x-generator` anchor takes the profiles from make, and the variables as make passes them:

```yaml
    SPRING_PROFILES_ACTIVE: ${PROFILES:?set by make, <transport>,<env>}
    KAFKA_BOOTSTRAP_SERVERS: ${KAFKA_BOOTSTRAP_SERVERS:-}
    KAFKA_API_KEY: ${KAFKA_API_KEY:-}
    # the other four the same way
```

### TRANSPORT in platform.mk

Owner: pair. Turns green: the make examples of the map.

```make
# where a story plays: cli, the console and the log; kafka, the broker of make kafka-up; confluent, Confluent Cloud
# with the keys from .env.<ENV>.private
TRANSPORT ?= kafka
PROFILES = $(TRANSPORT),$(ENV)
# the addresses of the local broker and registry on the flock's network
FLOCK_KAFKA := KAFKA_BOOTSTRAP_SERVERS=kafka:19092 SCHEMA_REGISTRY_REST_ENDPOINT=http://registry:8081
# the broker comes up first when the story plays on it
NEEDS = $(if $(filter kafka,$(TRANSPORT)),kafka-up)
COMPOSE := $(WITH_ENV) $(if $(filter kafka,$(TRANSPORT)),$(FLOCK_KAFKA)) PROFILES=$(PROFILES) VERSION=$(VERSION) docker compose

run: $(NEEDS)   ## from source on TRANSPORT, default kafka, with the topics of ENV, default test; ARGS pick the story and its settings
	@$(WITH_ENV) SPRING_PROFILES_ACTIVE=$(PROFILES) $(GRADLE) $(BOOT):bootRun $(if $(ARGS),--args="$(ARGS)")

##@ Kafka, the local broker and registry the kafka transport plays on
kafka-up:   ## start the broker and the registry, and register the schemas of ENV
	@$(COMPOSE) --profile kafka up -d --wait kafka registry
	@$(MAKE) --no-print-directory kafka-init

kafka-down: ## stop the broker and the registry; what they held is gone
	@$(COMPOSE) --profile kafka down
```

`docker-run`, `up`, `bdd`, and `bdd-published` take `$(NEEDS)` the same way; `docker-run` joins the flock's network
on `kafka`, and passes `CREDENTIALS` on `confluent`.

### kafka-init, the schemas on the local registry

Owner: pair. Turns green: the schema step of
[i_can_generate_events.feature](../../../platform/src/test/resources/features/i_can_generate_events.feature) on
`kafka`.

It registers what [iac/schemas.tf](../../../iac/schemas.tf) registers on Confluent Cloud: `<payload>.proto` under
`<env>.<payload>s-value`, the transaction with its imports as references.

```make
REGISTRY_URL := http://localhost:8081
SCHEMA_POST := curl -sS --fail-with-body --retry-connrefused --retry 30 --retry-delay 1 -H 'Content-Type: application/vnd.schemaregistry.v1+json' --data @-

kafka-init:   ## register the schemas of ENV on the local registry, as iac/ does on Confluent Cloud
	@for payload in product offer order; do \
	  jq -Rs '{schemaType: "PROTOBUF", schema: .}' src/main/proto/$$payload.proto \
	  | $(SCHEMA_POST) $(REGISTRY_URL)/subjects/$(ENV).$${payload}s-value/versions; echo; \
	done
	@jq -Rs --arg env $(ENV) '{schemaType: "PROTOBUF", schema: ., references: [ \
	    {name: "order.proto", subject: "\($$env).orders-value", version: 1}, \
	    {name: "offer.proto", subject: "\($$env).offers-value", version: 1}]}' src/main/proto/transaction.proto \
	  | $(SCHEMA_POST) $(REGISTRY_URL)/subjects/$(ENV).transactions-value/versions; echo
```

### The spec on a transport

Owner: pair for the Gradle wiring, themathmagician for the drivers. Turns green: every feature on `kafka`.

The `bdd` task in [platform.gradle:103](../../../platform.gradle#L103) passes the profiles and the broker's network
to the suite, from `make bdd`:

```groovy
        // where the image under test plays, -Pprofiles=<transport>,<env>, and the network of the local broker
        systemProperty 'bdd.profiles', providers.gradleProperty('profiles').getOrElse('kafka,test')
        systemProperty 'bdd.network', providers.gradleProperty('network').getOrElse('')
```

The drivers read them where they set `test`:
[GeneratorContainer.java:70](../../../platform/src/testFixtures/java/dk/mathmagicians/playground/confluent/eventing/bdd/GeneratorContainer.java#L70),
[StoryContainer.java:71](../../../platform/src/testFixtures/java/dk/mathmagicians/playground/confluent/eventing/bdd/StoryContainer.java#L71),
and the suite's own context at
[CucumberSpringConfiguration.java:17](../../../platform/src/testFixtures/java/dk/mathmagicians/playground/confluent/eventing/bdd/CucumberSpringConfiguration.java#L17).
`GeneratorContainer.credentials()`, at
[GeneratorContainer.java:87](../../../platform/src/testFixtures/java/dk/mathmagicians/playground/confluent/eventing/bdd/GeneratorContainer.java#L87),
passes the flock's addresses on `kafka` and the six variables on `confluent`.

## CI off Confluent

Owner: pair. Turns green: the PR's checks, and the CI rule of the map.

- `cicd.yaml`, job `cd`: `make bdd-published TAG=... TRANSPORT=kafka`, with the broker on the runner.
- `cicd-client-example.yaml`: the purse's features on `kafka`, per its question 2.
- `load-run.yaml`: `workflow_dispatch` with the inputs `transport`, a choice of `cli`, `kafka`, `confluent`, and the
  load arguments.

## Stories describe themselves

| Task | Owner | Turns green |
|---|---|---|
| `Story.publishes()` in each story | themathmagician | the unit tests of `publishes()` |
| `@PrimaryPort(name, description)` on each story | themathmagician | the architecture rule |
| The named key strategies | themathmagician | the map's key rule |

## Architecture discovery: one model, three levels

| Task | Owner | Turns green |
|---|---|---|
| The `model.dsl` writer, through `ModuleDocumentation` | themathmagician | the fixture test |
| `workspace.dsl` and `styles.dsl`: the forest, the trees, the hexagon | themathmagician | the three levels |
| The rendering in the build, `make arch-gen` | pair | `make arch-check`, exact |

## The skill generate-platform-client

| Task | Owner | Turns green |
|---|---|---|
| The skill, from the architecture spec | themathmagician | the client's acceptance |
| A rehearsal, frozen, and recorded as the fallback | themathmagician | the recording plays |
| The offline run from `make platform-install`, [Makefile:116](../../../Makefile#L116) | pair | the client builds without the network |

## Slides alongside

Owner: pair. The deck in [slides/](../../../slides/slides.md), with `make slides-present` and `make slides-pdf`. Each
idea fills its section of the deck as it lands.

- The talk plays in the `slides` service of [compose.yaml](../../../compose.yaml), from
  [slides/Dockerfile](../../../slides/Dockerfile); Node and Chromium live in the image. Spike played and signed
  2026-10-20.
- One compose plays Wonderland and the talk, signed 2026-10-20.
- The PDF in `docs/generated/slides` is the export of 2026-10-20.

## README

Owner: themathmagician, with the lines from pair.

- Configuration, Environments: the transports beside the environments.
- Spring Boot: secrets fail fast at startup, named by `ConfluentKeys`.
- Play and Definition of done: `make run TRANSPORT=cli`, and `make run-tiny` per its question 5.
- Tech Stack: Apache Kafka and the schema registry for the local and the CI runs.
- Git and CI: `load-run.yaml` on dispatch, `cd` and `cicd-client-example` on the local broker.
