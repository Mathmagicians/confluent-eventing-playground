# Automation

The tasks of each feature in the [catalog](release-0.0.7.md), each with its owner and the scenario it turns green,
and the core snippets. One go per change. The evidence of Spec green, the green run and the ADRs a feature touches,
is linked from the feature's row in the catalog.

## Messaging capability: cli, kafka, confluent

### Profiles: cli, kafka, confluent

Owner: pair for the properties, themathmagician for the annotations. Turns green: every scenario of the map.

[platform.properties](../../../platform/src/main/resources/platform.properties), shared by every distribution, holds
the default, `spring.profiles.default=cli`, at
[platform.properties:6](../../../platform/src/main/resources/platform.properties#L6), and takes the group:

```properties
# Confluent Cloud is Kafka with credentials
spring.profiles.group.confluent=kafka
```

`platform/src/main/resources/application-kafka.properties` is
[kafka.properties](../../../platform/src/main/resources/kafka.properties) with the local defaults, lines 7 to 18 and
23 to 39 as they are:

```properties
# the broker and the registry of make kafka-up, unless the environment names others
spring.kafka.bootstrap-servers=${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
spring.kafka.properties.schema.registry.url=${SCHEMA_REGISTRY_REST_ENDPOINT:http://localhost:8081}
```

`platform/src/main/resources/application-confluent.properties` holds what Confluent Cloud adds, and the values
`ConfluentKeys` checks, in place of its import of `kafka.properties`,
[application-confluent.properties:2](../../../platform/src/main/resources/application-confluent.properties#L2):

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

The console and log adapters carry `@Profile("cli")`. The Kafka adapters' annotations:

| File | Current | Replacement |
|---|---|---|
| [KafkaPublisher.java:26](../../../platform/src/main/java/dk/mathmagicians/playground/confluent/eventing/adapter/kafka/publisher/KafkaPublisher.java#L26), [StoryListener.java:26](../../../platform/src/main/java/dk/mathmagicians/playground/confluent/eventing/adapter/kafka/consumer/StoryListener.java#L26), [Reader.java:18](../../../platform/src/main/java/dk/mathmagicians/playground/confluent/eventing/adapter/kafka/consumer/Reader.java#L18), [DeadLetters.java:23](../../../platform/src/main/java/dk/mathmagicians/playground/confluent/eventing/adapter/kafka/consumer/DeadLetters.java#L23), [Topics.java:14](../../../platform/src/main/java/dk/mathmagicians/playground/confluent/eventing/adapter/kafka/Topics.java#L14) | `@Profile("confluent")` | `@Profile("kafka")` |

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
of question 6.

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
    image: confluentinc/cp-schema-registry:8.3.1   # question 6
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

Owner: pair. Turns green: the make examples of the map. `TRANSPORT`, `PROFILES`, and the scenarios each transport
carries are [platform.mk:39](../../../platform.mk#L39), from
[CI plays each transport](../this-pr/03_automation.md#ci-plays-each-transport); this feature adds `kafka-up` and the
flock's addresses.

```make
# the addresses of the local broker and registry on the flock's network
FLOCK_KAFKA := KAFKA_BOOTSTRAP_SERVERS=kafka:19092 SCHEMA_REGISTRY_REST_ENDPOINT=http://registry:8081
# the broker comes up first when the story plays on it
NEEDS = $(if $(filter kafka,$(TRANSPORT)),kafka-up)
COMPOSE := $(WITH_ENV) $(if $(filter kafka,$(TRANSPORT)),$(FLOCK_KAFKA)) PROFILES=$(PROFILES) VERSION=$(VERSION) docker compose

run: $(NEEDS)   ## from source on TRANSPORT and ENV; ARGS pick the story and its settings
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

The `bdd` task in [platform.gradle:115](../../../platform.gradle#L115) takes the transport and the profiles from
`make bdd`, and the suite and its containers play them; this feature adds the broker's network:

```groovy
        // the network of the local broker, -Pnetwork=<network>, from make on kafka
        systemProperty 'bdd.network', providers.gradleProperty('network').getOrElse('')
```

The drivers join it where they hand the containers the profiles:
[GeneratorContainer.java:78](../../../platform/src/testFixtures/java/dk/mathmagicians/playground/confluent/eventing/bdd/GeneratorContainer.java#L78)
and
[StoryContainer.java:62](../../../platform/src/testFixtures/java/dk/mathmagicians/playground/confluent/eventing/bdd/StoryContainer.java#L62).
`GeneratorContainer.credentials()`, at
[GeneratorContainer.java:103](../../../platform/src/testFixtures/java/dk/mathmagicians/playground/confluent/eventing/bdd/GeneratorContainer.java#L103),
passes the flock's addresses on `kafka`, beside the six variables `confluent` takes.

- On `kafka`, `CI_TRANSPORTS` of [CI plays each transport](../this-pr/03_automation.md#ci-plays-each-transport)
  takes `kafka` in, its broker on the runner, for the `bdd` job and the purse, per question 5.

### The console hears its story's region

| Task | Owner | Turns green |
|---|---|---|
| [ConsoleListener.java:258](../../../platform/src/main/java/dk/mathmagicians/playground/confluent/eventing/adapter/cli/console/ConsoleListener.java#L258) hands the story what `story.hears`, as [StoryListener.java:64](../../../platform/src/main/java/dk/mathmagicians/playground/confluent/eventing/adapter/kafka/consumer/StoryListener.java#L64) does | themathmagician | `ConsoleListenerTest`: an envelope of another region is not heard |

## The catalog follows the board

`project.yaml` plays [Project sync](../this-pr/03_automation.md#project-sync-the-board-follows-the-catalog) on every
push, and the board's moves back into the catalog.

| Task | Owner | Turns green |
|---|---|---|
| `make project-links`: the catalogs committed and pushed when the sync changed them | pair | the links on the branch |
| `.github/workflows/project.yaml`: on `docs/releases/**` but `archived/**`, with `GITHUB_TOKEN`, issues and contents write | pair | the run green |

## A release ships what is ready

Owner: pair.

- `@wip` and `@manual` stay out of the run, 2026-10-21: the `bdd` task at
  [platform.gradle:126](../../../platform.gradle#L126), `SCENARIOS` in [platform.mk](../../../platform.mk), and the
  README's Testing. `make bdd-snippets` played the 17 scenarios, `SCENARIOS=@nothing` none.
- Open: the root `bdd`, which parses `src/test/resources/features/` and plays none of its `@manual` scenarios.

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
feature fills its section of the deck as it lands. The talk plays from compose,
[Slides spike](../this-pr/03_automation.md#slides-spike-the-talk-plays-from-compose).

- The PDF in `docs/generated/slides` is the export of 2026-10-20.

## README

Owner: themathmagician, pair secretary.

- Configuration, Environments: `kafka` in the transports' table.
- Spring Boot: secrets fail fast at startup, named by `ConfluentKeys`.
- Play: `make run TRANSPORT=kafka`, `make kafka-up`, and `make kafka-down`.
- Tech Stack: Apache Kafka and the schema registry for the local and the CI runs.
- Git and CI: `bdd (kafka)` and `cicd-client-example` on the local broker.
