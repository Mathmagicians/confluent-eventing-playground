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
- GitHub Actions - CICD + publish to GH registry, and hourly load runs 
- Container image - deployment unit, built with `./gradlew bootBuildImage`

## Purpose

- Playground for refreshing event-streaming fundamentals against a managed Confluent cluster: keys, partitions, ordering, idempotence, schemas.
- Generate repeatable load from GitHub Actions (hourly cron) and from a developer machine.
- Use stream processing capabilities from Confluent to consume over several topics

One topic per payload type, each keyed for a different ordering guarantee. Kafka keeps the records of one partition
in order, and the key picks the partition, so records with the same key stay in order.

```
 products   key = product     offers   key = region + product    orders   key = region

 p0 | P-POCK P-POCK P-POCK    p0 | EMEA/P-POCK EMEA/P-POCK       p0 | EMEA EMEA EMEA EMEA
 p1 | P-FANN P-FANN           p1 | APAC/P-POCK APAC/P-POCK       p1 | APAC APAC
 p2 | P-TEAS                  p2 | EMEA/P-FANN                   p2 | AMER AMER AMER

 ordered per product          ordered per product in a region    ordered per region

 transactions   key = region + product + customer

 p0 | EMEA/P-POCK/C-01 EMEA/P-POCK/C-01
 p1 | APAC/P-POCK/C-07
 p2 | EMEA/P-FANN/C-01

 ordered per customer, per product in a region
```

## Architecture

### Services

| Service           | Role                                                                                                                                  | Runs where                              |
|-------------------|---------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------|
| `load-generator`  | Produces one payload type to its topic. One codebase, many instances distinguished by type and region (`EMEA`, `AMER`, `APAC`, ...). | Compose swarm, GitHub Actions cron      |
| `stream-consumer` | Consumes the topics, verifies ordering per key, exposes counters.                                                                     | Compose swarm                           |

Both are Spring Boot applications. Actuator health and metrics are the endpoints they expose.

### Domain

A payload is a `Product`, an `Offer`, an `Order`, or a `Transaction`, a sealed set. An event is an `Envelope` with a
payload.

- **The key picks the partition**, one key per topic as shown under Purpose. The default partitioner (murmur2 over
  the serialized key) maps a key to a partition by partition count, so the partition count of a topic is fixed at
  topic creation.
- `transactions` are keyed by region, product id, and customer id: region and product from the offer the
  transaction settles, the customer from the transaction, so a customer's transactions for one product in one
  region stay in order.
- Serialization: Protobuf via Confluent Schema Registry.

### Data flow

```
 GitHub Actions cron (hourly)          developer machine
          |                                   |
          v                                   v
   load-generator (EMEA)   ...   load-generator (region N)
          |   key per topic, value = Envelope
          v
   Confluent Cloud   topics: products, offers, orders, transactions, prefixed test. or prod.   (N partitions, fixed)
          |
          v
   stream-consumer   per-partition ordering checks, metrics
```

### Environments

| Profile | Publishes to    | Runs from                                                      |
|---------|-----------------|----------------------------------------------------------------|
| `local` | the log         | Developer machine, no credentials                              |
| `test`  | `test.<topic>`  | Developer machine, the `cd` job on every pull request          |
| `prod`  | `prod.<topic>`  | `load-run.yaml` hourly cron, developer machine with `ENV=prod` |

`test` and `prod` run against Confluent Cloud.

## Repository layout

```
.
├── README.md                 this file: project and coding standards
├── Makefile                  single entry point for humans and CI
├── compose.yaml              the swarm: load generators per region, the consumer
├── build.gradle / settings.gradle
├── iac/                      Terraform: the topics on the Confluent cluster, applied by Terraform Cloud
├── .github/workflows/        cicd.yaml, load-run.yaml, iac.yaml
├── common/                   Order domain, serialization, shared test fixtures
│   ├── src/main/proto/       Protobuf schemas
│   └── src/generated/        protoc output, committed, regenerated with make proto-gen
├── load-generator/           producer service
└── stream-consumer/          consumer service
```

Root package: `dk.mathmagicians.playground.confluent`. Packages by layer inside a service: `domain` in the centre,
`dto`, `cli`, and the Kafka adapter at the edge. The domain is one package, so a sealed type and its records stay
package-private neighbours.

Tests live next to what they test:

```
src/test/java/...                unit tests, one class per production class
src/test/resources/features/     Gherkin features
src/test/java/.../bdd/           step definitions and test drivers
```

## Getting started

Prerequisites: JDK 25, Docker, the `gh` CLI for pipeline work, a Confluent Cloud API key.

The Makefile is the entry point for humans and CI. `make help` lists the targets by section, `make check` is the
CI gate.

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
| `REGION`                                                  | compose          | Region stamped on every event, default EMEA                                     |
| `TTL`                                                     | compose          | Seconds a generator runs, default 60, max 300                            |
| `TF_CLOUD_ORGANIZATION` / `TF_WORKSPACE`                  | `make tf-*`      | Terraform Cloud workspace holding the state                              |
| `TF_TOKEN_app_terraform_io`                               | `iac.yaml`       | Terraform Cloud token; a developer machine has `terraform login` instead |

Secrets live in two GitHub environments, `confluent-test` and `confluent-prod`, the same Confluent cluster and API
key, one topic prefix each. Locally the same six variables live in `.env.test.private` and `.env.prod.private`,
git-ignored. `make` sources the file for `ENV`, default `test`, into the command it runs and nothing else, so your
shell never carries them. Properties files, Gherkin, and test fixtures refer to them by variable name.

The Terraform Cloud workspace holds `KAFKA_ID`, `KAFKA_REST_ENDPOINT`, and the Kafka API key as workspace variables.
Plans and applies run there, from its GitHub connection to `iac/`. A third GitHub environment, `terraform-cloud`,
holds the Terraform Cloud token as `TF_TOKEN_app_terraform_io`, the organization, and the workspace name for
`iac.yaml`, each secret named as the variable it becomes.

## Play

The swarm, one generator per payload type with defaults from `compose.yaml`, is the Swarm section of `make help`.

The image on its own, defaults from `application.properties`:

```bash
docker run --rm confluent-eventing-playground:$(make version)
docker run --rm confluent-eventing-playground:$(make version) --load.type=order --load.concurrent=20 --load.interval=100 --load.region=APAC --load.ttl=120
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

- Configuration as `@ConfigurationProperties` records, validated in the compact constructor with a message that
  carries the value, injected where needed.
- Auto-configuration first. A `@Bean` method covers what auto-configuration cannot express and carries a one-line
  comment saying so.
- Profiles are `local`, `test`, and `prod`. `local` publishes every envelope to the log at INFO and needs no
  credentials, the other two publish to Kafka and differ in properties: the topic names.
- `application.properties` holds defaults, `application-<profile>.properties` holds the profile. Secrets are
  `${ENV_VAR}` placeholders that fail fast when unset. Everything else has a default.
- `@SpringBootTest` serves one wiring test per service and the BDD suite.
- Spring Boot 4 specifics: `@MockitoBean` and `@MockitoSpyBean` for test doubles in the context. Nullness follows
  JSpecify.

### Kafka and Confluent Cloud

- Producer: `acks=all`, `enable.idempotence=true`, compression `lz4` or `zstd`, explicit `linger.ms` and `batch.size`.
  All of it through Spring properties, each tuning value with a comment.
- Every record has a key.
- Topics are created by Terraform Cloud from `iac/`. Test containers auto-create.
- Topic names are `<env>.<topic>`, the environment `test` or `prod` first: `test.orders`. The dot is the only
  separator.
- Consumer group id is explicit and named after the service. Offset management stays on Spring defaults until a
  scenario needs otherwise.
- Listener exceptions propagate to Spring's `DefaultErrorHandler`, which publishes to the dead-letter topic
  `<topic>.DLT`, Spring's default name and partition, through `DeadLetterPublishingRecoverer`.
- One serialization class per direction owns `byte[]` and serializer configuration. Business code works with
  `Envelope`.
- Every message on the wire is an `Envelope`, the payload packed as `google.protobuf.Any`. The envelope schema stays
  the same when a payload type is added.
- Schema evolution: `BACKWARD` compatibility, `TopicNameStrategy`, schemas checked in under
  `common/src/main/proto`.
- Confluent Cloud clients use `SASL_SSL` with `PLAIN`. Every other setting stays at the Confluent-recommended default
  until a measurement justifies a change.

### Testing

Test layers:

| Layer | Tool                                              | Scope                                          | Speed   |
|-------|---------------------------------------------------|------------------------------------------------|---------|
| Unit  | JUnit 5, AssertJ, Mockito                         | One class in isolation                         | ms      |
| BDD   | Cucumber, Testcontainers running the image        | One feature end to end: the image against the Confluent test cluster | seconds |
| Load  | GitHub Actions against Confluent Cloud            | the latest image, hourly, measured             | minutes |

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
- Log lines carry identifiers and counts. Payloads appear at `DEBUG`, stack traces at `ERROR`. The `local` profile
  is the exception: the log is its publisher, so every envelope appears at `INFO`.
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
- Every PR has a green `ci` and `cd` and a review before a human merges.
- The version is Gradle's, derived from git tags: `1.2.3` at tag `v1.2.3`, `1.2.4-SNAPSHOT` after it,
  `0.0.1-SNAPSHOT` before the first tag. `make version` and `make next-version` print them.
- `cicd.yaml`, job `ci`, runs on pull requests to `main`, on pushes to `main`, and on `workflow_dispatch`:
  proto-check, build with unit tests, container image, then publishes the build to
  `ghcr.io/mathmagicians/confluent-eventing-playground` as a candidate tagged `sha-<short sha>`. A pull request
  adds `pr-<number>`, a push to `main` adds `latest`. Only `main` moves `latest`.
- `cicd.yaml`, job `cd`, follows `ci`: it deploys to test by running the integration tests, `make bdd-published`,
  against the candidate, with the credentials of the `confluent-test` environment. A green `cd` on a pull request
  is what says the build can be promoted.
- `cicd.yaml`, job `tag`, follows `cd` on `main`: `make git-release` puts a git tag `v<version>` on the tested
  commit and the same version on the candidate image in the registry. `make git-tag` is the git part alone. The version is Gradle's next, or the
  `workflow_dispatch` input, e.g. `0.1.0`.
- `load-run.yaml` runs hourly (`0 * * * *`) and on `workflow_dispatch` with one input, the load arguments. It
  deploys to prod: the `latest` image, `make docker-smoke` when the arguments are empty and `make docker-run` otherwise, with the
  credentials of the `confluent-prod` environment. No `latest` image, no run.
- `iac.yaml` runs on pull requests to `main`, on pushes to `main`, and on `workflow_dispatch`: `make tf-check`,
  then `make tf-plan`, a speculative plan in Terraform Cloud written to the job summary, with the credentials of
  the `terraform-cloud` environment. Terraform Cloud applies on `main` from its GitHub connection to `iac/`.
- `main` is protected: changes arrive by pull request with a green `ci` and `cd`, no force pushes, linear history.
  `.github/branch-protection.json` is the setting, `make gh-main-protection` applies it.

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
- [ ] Topics and dead-letter topics created by Terraform Cloud from `iac/`
- [ ] workers can publish against test.* kafka topics
- [ ] Prod docker swarm works against prod.* kafka topics
- [ ] Cucumber wired into the build (JUnit Platform Suite, Testcontainers for workers, Confluent test.* topics)
- [ ] BDD feature: I can publish messages
- [ ] BDD feature: same key ends up in the same partition
- [ ] Partition key per topic
- [ ] Publish 1 000 000 messages to Confluent Cloud
- [ ] Stream consumer service
- [ ] Protobuf via Schema Registry
- [ ] Split into modules, convert to hexagonal
- [x] GitHub Actions `cicd.yaml`
- [x] GitHub Actions `load-run.yaml`, hourly cron
