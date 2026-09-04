# Event Streaming Refresher

Reference implementation of a Kafka **load generator** and **stream consumer** running against **Confluent Cloud**.

## Tech Stack
- Java 25 - newest LTS, toolchain pinned in `build.gradle`, finalized features
- Spring Boot 4.1.1 + Gradle 9.7.1 (Groovy DSL) - micro service framework, Spring Framework 7, wrapper committed
- Spring for Apache Kafka - producer and consumer, version from the Boot BOM
- Confluent Cloud - Kafka + Schema Registry
- Cucumber BDD - for black box testing, JUnit Platform engine, version pinned in `build.gradle`
- JUnit 5, AssertJ, Mockito - unit tests, versions from the Boot BOM
- Testcontainers - Kafka container for BDD, version from the Boot BOM
- Docker + Compose - starts the swarm of load generators and the consumer
- GitHub Actions - CI and hourly load runs
- Container image - deployment unit, built with `./gradlew bootBuildImage`

## Purpose

- Playground for refreshing event-streaming fundamentals against a managed Confluent cluster: keys, partitions, ordering, idempotence, schemas.
- Generate repeatable load from GitHub Actions (hourly cron) and from a developer machine.

## Architecture

### Services

| Service           | Role                                                                                                                                  | Runs where                              |
|-------------------|---------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------|
| `load-generator`  | Produces `Order` events to the `orders` topic. One codebase, many instances distinguished by region (`EMEA`, `AMER`, `APAC`, ...).    | Compose swarm, GitHub Actions cron      |
| `stream-consumer` | Consumes `orders`, verifies per-product ordering, exposes counters.                                                                   | Compose swarm                           |

Both are Spring Boot applications. Actuator health and metrics are the endpoints they expose.

### Domain

`Order(product, quantity, price)` is the single event type.

- **Partition key is `product`.** All orders for one product land on one partition, so per-product order is preserved.
  The default partitioner (murmur2 over the serialized key) maps a key to a partition by partition count, so the
  partition count of `orders` is fixed at topic creation.
- Serialization: Protobuf via Confluent Schema Registry.

### Data flow

```
 GitHub Actions cron (hourly)          developer machine
          |                                   |
          v                                   v
   load-generator (EMEA)   ...   load-generator (region N)
          |   key = product, value = Order
          v
   Confluent Cloud   topic: orders-<profile>   (N partitions, fixed)
          |
          v
   stream-consumer   per-partition ordering checks, metrics
```

### Environments

| Profile | Topic           | Runs from                        |
|---------|-----------------|----------------------------------|
| `local` | `orders-local`  | Developer machine, compose swarm |
| `prod`  | `orders-prod`   | GitHub Actions, hourly cron      |

Both run against Confluent Cloud.

## Repository layout

```
.
├── README.md                 this file: project and coding standards
├── Makefile                  single entry point for humans and CI
├── compose.yaml              the swarm: load generators per region, the consumer
├── build.gradle / settings.gradle
├── .github/workflows/        cicd.yaml, load-run.yml
├── common/                   Order domain, serialization, shared test fixtures
│   ├── src/main/proto/       Protobuf schemas
│   └── src/generated/        protoc output, committed, regenerated with make proto-gen
├── load-generator/           producer service
└── stream-consumer/          consumer service
```

Root package: `dk.mathmagicians.playground.confluent`. Sub-packages by feature: `order`, `load`, `consumer`.

Tests live next to what they test:

```
src/test/java/...                unit tests, one class per production class
src/test/resources/features/     Gherkin features
src/test/java/.../bdd/           step definitions and test drivers
```

## Getting started

Prerequisites: JDK 25, Docker, the `gh` CLI for pipeline work, a Confluent Cloud API key.

```bash
make check                 # the CI gate: proto-check, build, image, bdd, in that order
make build                 # compile, unit tests, jar
make test                  # unit tests
make image                 # container image confluent-eventing-playground:0.0.1-SNAPSHOT
make bdd                   # Cucumber with Testcontainers, against the image
make bdd-snippets          # step-definition snippets for undefined steps
make proto-gen             # regenerate src/generated from the schemas
make publish REGISTRY=ghcr.io/<owner> TAGS="0.0.1-SNAPSHOT latest"   # push the image
```

CI calls the same targets.

### Configuration

All environment-specific values come from environment variables, bound through `${...}` placeholders in the profile
properties files. Names are `UPPER_SNAKE`, prefixed by concern.

| Variable                                                  | Used by          | Meaning                                                                  |
|-----------------------------------------------------------|------------------|--------------------------------------------------------------------------|
| `KAFKA_BOOTSTRAP_SERVERS`                                 | both services    | Confluent Cloud bootstrap endpoint                                       |
| `KAFKA_API_KEY` / `KAFKA_API_SECRET`                      | both services    | SASL/PLAIN credentials                                                   |
| `SCHEMA_REGISTRY_URL`                                     | both services    | Schema Registry endpoint                                                 |
| `SCHEMA_REGISTRY_API_KEY` / `SCHEMA_REGISTRY_API_SECRET`  | both services    | Schema Registry basic auth                                               |
| `PRODUCT_CONCURRENT`, `OFFER_CONCURRENT`, `ORDER_CONCURRENT` | compose       | Producers per generator, default 10                                      |
| `PRODUCT_INTERVAL`, `OFFER_INTERVAL`, `ORDER_INTERVAL`    | compose          | Milliseconds a producer sleeps between events, default 250               |
| `REGION`                                                  | compose          | Region stamped on every event, default EMEA                              |
| `TTL`                                                     | compose          | Seconds a generator runs, default 60, max 300                            |

Secrets live in GitHub Actions secrets in CI and in a git-ignored `.env` locally. Properties files, Gherkin, and test
fixtures refer to them by variable name.

## Play

The swarm, one generator per event type, defaults from `compose.yaml`:

```bash
make image                 # once, or after a code change
make up                    # product, offer, and order generators
make up-offer              # one generator; also up-product, up-order
OFFER_CONCURRENT=50 OFFER_INTERVAL=100 TTL=300 make up-offer
REGION=APAC make up
make down
```

The image on its own, defaults from `application.properties`:

```bash
docker run --rm confluent-eventing-playground:0.0.1-SNAPSHOT
docker run --rm confluent-eventing-playground:0.0.1-SNAPSHOT --load.type=order --load.concurrent=20 --load.interval=100 --load.region=APAC --load.ttl=120
docker run --rm ghcr.io/mathmagicians/confluent-eventing-playground:latest --load.type=product
```

| Argument            | Values                    | Default |
|---------------------|---------------------------|---------|
| `--load.type`       | `offer`, `order`, `product` | offer |
| `--load.concurrent` | producers running the loop | 10     |
| `--load.interval`   | milliseconds a producer sleeps between events | 250 |
| `--load.region`     | stamped on every event    | EMEA    |
| `--load.ttl`        | seconds to run, max 300   | 60      |

A wrong value fails startup with the reason.

## Coding standards

A review finding cites the rule it breaks.

### Principles

1. **DRY, then readable.** Extract when a second copy of the same knowledge appears.
2. **SOLID.** One responsibility per class, narrow interfaces, dependencies injected through the constructor.
3. **Functional style.** Immutable data, pure functions, side effects at the edges: Kafka, clock, logging.
4. **Behaviour first.** A change starts with the Gherkin scenario or unit test that describes it.
5. **Hexagonal architecture.** Domain in the centre, Kafka and Spring in adapters at the edges.

### Java 25

- Toolchain is Java 25, finalized features. Expected wherever they fit:
  - `record` for every value type: events, configuration properties, test data.
  - `sealed` interfaces with exhaustive `switch` pattern matching for closed hierarchies.
  - Record deconstruction patterns in `switch` and `if`.
  - Unnamed variables `_` for ignored lambda parameters and catch variables.
  - Virtual threads for blocking work, enabled with `spring.threads.virtual.enabled=true`.
- `var` for locals when the right-hand side names the type, an explicit type otherwise.
- `Optional` is a return type. Fields, parameters, and collection elements use the plain type.
- Public boundaries express absence with `Optional`, an empty collection, or a sealed result type. JSpecify
  `@Nullable` marks where `null` crosses a boundary.
- The domain throws unchecked, domain-specific exceptions. Checked exceptions are wrapped at the boundary where they
  arise.
- All output goes through SLF4J.
- Behaviour lives on the type that owns the data.

### Spring Boot

- Configuration as `@ConfigurationProperties` records with `@Validated` constraints, injected where needed.
- Auto-configuration first. A `@Bean` method covers what auto-configuration cannot express and carries a one-line
  comment saying so.
- Profiles are `local` and `prod`. Differences between them live in properties.
- `application.properties` holds defaults, `application-<profile>.properties` holds the profile. Secrets are
  `${ENV_VAR}` placeholders that fail fast when unset. Everything else has a default.
- `@SpringBootTest` serves one wiring test per service and the BDD suite.
- Spring Boot 4 specifics: `@MockitoBean` and `@MockitoSpyBean` for test doubles in the context. Nullness follows
  JSpecify.

### Kafka and Confluent Cloud

- Producer: `acks=all`, `enable.idempotence=true`, compression `lz4` or `zstd`, explicit `linger.ms` and `batch.size`.
  All of it through Spring properties, each tuning value with a comment.
- Every record has a key.
- Topics are created by infrastructure. Test containers auto-create.
- Consumer group id is explicit and named after the service. Offset management stays on Spring defaults until a
  scenario needs otherwise.
- Listener exceptions propagate to Spring's `DefaultErrorHandler`, which publishes to the dead-letter topic
  `<topic>.dlt` through `DeadLetterPublishingRecoverer`.
- One serialization class per direction owns `byte[]` and serializer configuration. Business code works with `Order`.
- Schema evolution: `BACKWARD` compatibility, `TopicNameStrategy`, schemas checked in under
  `common/src/main/proto`.
- Confluent Cloud clients use `SASL_SSL` with `PLAIN`. Every other setting stays at the Confluent-recommended default
  until a measurement justifies a change.

### Testing

Test layers:

| Layer | Tool                                              | Scope                                          | Speed   |
|-------|---------------------------------------------------|------------------------------------------------|---------|
| Unit  | JUnit 5, AssertJ, Mockito                         | One class in isolation                         | ms      |
| BDD   | Cucumber, Spring Boot test, Testcontainers Kafka  | One feature end to end against Kafka in Docker | seconds |
| Load  | GitHub Actions against Confluent Cloud            | `LOAD_MESSAGE_COUNT` orders per run, measured  | minutes |

Unit tests:

- One test class per production class, same package, named `<Class>Test`.
- Method names describe behaviour in the present tense: `rejectsNegativeQuantity`.
- Arrange, act, assert, separated by blank lines. One behaviour per test.
- AssertJ for every assertion.
- Mock what you own and what does I/O. Records, collections, and Kafka client types come from the fixtures.
- Test data comes from builders or factory methods in `*Fixtures` classes.

BDD with Cucumber:

- A feature file describes one capability in domain language. Topics and partitions are domain concepts in this
  project and belong in features. Class names, serialization formats, ports, and client configuration belong in step
  definitions and drivers.
- `Given` sets up state, `When` is one action, `Then` asserts an observable outcome. Up to three `And` steps per
  keyword.
- `Scenario Outline` for variations of one behaviour. Separate scenarios for separate behaviours.
- Step definitions are glue: one line delegating to a test driver class. Assertions live in the driver.
- Steps are shared across features. Search for an existing step before writing one.
- Tags: `@wip` (runs locally), `@slow`, `@cloud` (runs where credentials are present).
- Features run through the JUnit Platform Suite engine as part of `make check`. A red feature blocks the build.

### Naming and structure

- Classes are nouns naming a domain concept or an actor: `Order`, `OrderProducer`.
- Methods are verbs. Boolean methods read as predicates: `isOrdered`, `hasKey`.
- Constants are `UPPER_SNAKE`, defined in the type that uses them.
- One public type per file. Package-private by default, `public` when another package needs it.
- A method fits on one screen.

### Logging and observability

- SLF4J with parameterised messages: `log.info("Produced {} orders for {}", count, region)`.
- `ERROR` means a human should look. `WARN` means degraded but running. `INFO` is lifecycle and counts. `DEBUG` is
  per-message detail, off by default.
- Log lines carry identifiers and counts. Payloads appear at `DEBUG`, stack traces at `ERROR`.
- Micrometer counters and timers for produced and consumed records.

### Dependencies and build

- Versions come from the Spring Boot BOM. A hard-coded version carries a comment with the reason and the date.
- Project code compiles with zero warnings under `-Xlint:all -Werror`.
- Formatting: Spotless with google-java-format, AOSP style (4 spaces). `./gradlew spotlessApply` before commit, CI
  runs `spotlessCheck`.
- The Protobuf serializer comes from the Confluent Maven repository (`https://packages.confluent.io/maven/`).

### Git and CI

- Default branch `main`. Short-lived branches: `feat/<topic>`, `fix/<topic>`, `chore/<topic>`.
- Conventional Commits: `feat:`, `fix:`, `test:`, `chore:`, `docs:`, `ci:`, `build:`. Imperative subject under 72
  characters. The body says why.
- Every PR has a green `cicd.yaml` and a review before a human merges.
- `cicd.yaml` runs on pull requests to `main` and on pushes to `main`: `make check`, which is proto-check, build
  with unit tests, container image, then Cucumber with Testcontainers against that image. A push to `main` also
  publishes the image to `ghcr.io/mathmagicians/confluent-eventing-playground` tagged with the version, the short
  sha, and `latest`.
- `main` is protected: changes arrive by pull request with a green `check`, no force pushes, linear history.
- `load-run.yml` runs hourly (`0 * * * *`) and on `workflow_dispatch`: builds the load generator and produces
  `LOAD_MESSAGE_COUNT` orders to Confluent Cloud using repository secrets.

## Definition of done

- Unit tests cover the new class or the changed branch.
- `make check` passes locally.
- Every new dependency is noted in the PR.
- README updated if a standard, variable, or architectural decision changed.
- Review findings above Nit are resolved or explicitly deferred with a reason.

## Roadmap

- [x] Install Java 25
- [x] Hello world
- [x] Gradle script builds and runs the unit test
- [x] Makefile with `build`, `test`, `bdd`, `run`, `check`
- [ ] Local docker compose spins up a swarm of workers
- [ ] workers can generate load, and convert it to protobuf
- [ ] workers can publish against *-local kafka topics
- [ ] Prod docker swarm works against *-prod kafka topics
- [ ] Cucumber wired into the build (JUnit Platform Suite, Testcontainers for workers, Confluent *-test topics)
- [ ] BDD feature: load generator produces `Order(product, quantity, price)`
- [ ] BDD feature: same product ends up in the same partition
- [ ] Partition key on product
- [ ] Publish 1 000 000 messages to Confluent Cloud
- [ ] Stream consumer service
- [ ] Protobuf via Schema Registry
- [ ] Split into `common`, `load-generator`, `stream-consumer` modules
- [ ] GitHub Actions `cicd.yaml`
- [ ] GitHub Actions `load-run.yml`, hourly cron
