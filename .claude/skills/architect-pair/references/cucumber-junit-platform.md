# Cucumber on the JUnit Platform

## Runner and reports

- `@Suite @IncludeEngines("cucumber") @SelectPackages("features")` with the glue package as a configuration
  parameter. `@SelectClasspathResource` on a directory named like a package earns a warning.
- `junit-platform.properties` carries the plugins: `pretty` for the console, `summary` for the snippets, `html:`
  and `json:` for the report, `pretty:<file>` for a text the job summary can carry. Without `summary` the engine
  prints no snippets. `cucumber.snippet-type=camelcase`, `cucumber.ansi-colors.disabled=true`,
  `cucumber.publish.quiet=true`.
- `-PdryRun` sets `cucumber.execution.dry-run`: every step undefined, the reports still written, no credentials
  needed.
- Reports under `build/reports/cucumber/`; the `bdd` job prints `pretty.txt` in its summary and uploads the
  directory as an artifact.

## The Spring context

- `cucumber-spring` demands one glue class with `@CucumberContextConfiguration`. Here it carries
  `@SpringBootTest(classes = itself, webEnvironment = NONE)`, the `test` profile, `spring.main.lazy-initialization`,
  `@ImportAutoConfiguration(KafkaAutoConfiguration.class)`, and `@Import` of the drivers.
- Lazy initialization is what keeps the dry run credential-free: a missing variable fails at the first step that
  uses it, with the variable's name.
- The context boots per run and prints Spring's banner; it is the empty context, not the application.

## Glue and drivers

- Step classes are `public` with a `public` constructor; Cucumber needs that. Glue scope is one instance per
  scenario, so state between steps, a receipt, a record, a subject, lives in the step class as fields.
- Steps are one line delegating to a driver. Drivers are package-private beans, constructor-injected, and hold the
  assertions. A driver keeps long-lived clients, the admin client, the registry client, for the life of the context
  and closes them in `@PreDestroy`; a consumer is built per read.
- A parameter type with capture groups tells alternatives apart, which optional text and `a/b` cannot:
  `@ParameterType("topic (\\w+)|dead-letter topic for (\\w+)")` resolves the feature's word through the profile's
  `topics.*` property to the name on the cluster. The keyword is free, one step text serves `Given`, `When`, `Then`.
- Clients configure from `KafkaProperties`: `buildAdminProperties()`, `buildConsumerProperties()`, and
  `getProperties()` for the registry client, the same settings the serializer uses.

## The image under test

- `GenericContainer` on the `bdd.image` property, the credentials passed through by name, the profile as an env var,
  the load arguments as the command, `OneShotStartupCheckStrategy` so `start()` returns when the generator exited,
  logs to the test log through `Slf4jLogConsumer`.
- The receipt is a log line at DEBUG, `Published <id> to <topic>-<partition> at offset <offset>`, parsed from
  `getLogs()`.
- Reading back: assign the partition, seek to the offset, poll once, values as bytes. The envelope is in the headers,
  `ce_id`, `ce_region`, `ce_source`, `ce_time`. The value starts with the magic byte and the schema id, compared with
  the registry's latest id for the subject.
- Schema equality goes through `ProtobufSchema.canonicalString()` on both sides, references resolved from the
  registry by subject and version.

## A story under test

- `StoryContainer`, scenario-scoped, starts the image with `--story`, the story's settings as `--<name>.<key>`,
  the group `<story>-testrun-<salt>` through `spring.kafka.consumer.group-id`, which `StoryListener` honours over
  the story's own, and `auto-offset-reset=latest`. It waits for "partitions assigned" and records the topics' end
  offsets at start.
- Isolation is a salt: `Region`, scenario-scoped, salts the feature's region word, `EMEA-<8 hex>`, and the group.
  A story with a region hears its region only, so scenarios share the topics and never each other's records.
- Caught up means the group's committed offsets reached the end offsets, or nothing new was written since the
  start. Awaitility polls with `pollInSameThread()`: a scenario-scoped bean is not visible from another thread.
- The container stops with SIGTERM, `stopContainerCmd(...).withTimeout(20)`, before `stop()`, so the consumer
  leaves the group; the group is deleted afterwards, retried while "The group is not empty".
- What the scenario published is read back once into `Published`, receipts and payloads. The oracle is what the
  scenario placed, the result is the topic: an order is settled when a transaction names it.
- The platform's steps are the technology: the keys, the generator, the streams of offers, orders, and
  transactions, the reading. A story's glue adds its own outcomes through a driver like `TeaPartyTable`, and its
  README carries the table: scenarios, unique steps, of the platform, of the story.
