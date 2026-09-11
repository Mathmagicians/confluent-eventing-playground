# The purse, a story of its own

[![cicd-client-example](https://github.com/Mathmagicians/confluent-eventing-playground/actions/workflows/cicd-client-example.yaml/badge.svg?branch=main)](https://github.com/Mathmagicians/confluent-eventing-playground/actions/workflows/cicd-client-example.yaml)

A story is a business process the eventing platform runs: it says what it listens to, the platform hands it every
message of those kinds, and what it wants to publish, it publishes through the platform. This folder is one story,
Alice's purse, built as a client of the platform would build it: its own Gradle build, its own image, its own
feature, its own CI. Copy it, rename it, and you have your own.

## What you get from the platform

| Artifact                                                    | What it holds                                                                       |
|-------------------------------------------------------------|-------------------------------------------------------------------------------------|
| `dk.mathmagicians.playground:platform`                      | the domain, the `Story` contract, the Kafka and log adapters, the main class        |
| `dk.mathmagicians.playground:platform`, test fixtures       | the BDD drivers and steps, the parameter types, the architecture tests and the hexagon writer |
| `platform.gradle`                                           | the Gradle conventions: version from git tags, repositories, the Boot BOM, the tasks |
| `platform.mk`                                               | the make targets: build, test, image, features, architecture                        |
| `ghcr.io/mathmagicians/confluent-eventing-playground`       | the platform's own distribution, the generators and the tea party your story trades with |

The jars are on GitHub Packages, published by the platform's CI: a release under its version, `0.1.0`, and the
snapshot of the next one between releases. GitHub Packages needs a token even to read, so `GITHUB_ACTOR` and
`GITHUB_TOKEN` are in the environment, `gh auth token` on a developer machine with the `read:packages` scope.

## Set up your own

1. **Copy this folder** to a repository of its own. Put `platform.gradle` and `platform.mk` next to it, or point
   `platformGradle` in `gradle.properties` and `PLATFORM_MK` in the Makefile at a checkout of the platform.
2. **Name your build.** `settings.gradle` names the project, the Makefile names the image, `application.properties`
   names the application and the story it plays.
3. **Pin the platform.** `platformVersion` in `gradle.properties`, a version GitHub Packages holds.
4. **Write the story.** A class implementing `Story`, in a package marked `@Module(name = ...)`, so it shows up on
   the hexagon with its name:
   - `name()`, the word on the command line, `--story=purse`, and the consumer group
   - `listensTo()`, the payload types, `Set.of(Transaction.class)`; empty for a story that only publishes
   - `on(Envelope)`, one message of those kinds at a time; what you throw sets the message aside on the dead-letter
     topic
   - `start()`, once when the platform is up, for a story that acts on its own
   - `ttl()`, how long it plays before the platform ends the process; zero, the default, is until stopped
   - to publish, build a `PublishMessage` from the platform's `Publisher` and `Clock` in the auto-configuration,
     the tea party story in `stories/tea-party` shows how
5. **Give it settings.** A `@ConfigurationProperties("<name>")` record with a default for every value and a
   validating constructor: `--purse.owners="Alice:1000,White Rabbit:500"`, one entry per purse. A wrong value fails
   the start with the entry in the message.
6. **Join the platform.** An `@AutoConfiguration` class that binds the settings and makes the story a bean, listed
   in `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`. That is the whole
   contract: a jar Boot finds.
7. **Write the feature.** Under `src/test/resources/features`, in the language of the story. The platform's steps
   are yours to reuse, the cluster and the schemas, and so are its drivers, the cluster reader, the registry, the
   generator run as a container; the story's own steps go next to `RunCucumberTest`, whose glue names both
   packages.
8. **Keep the architecture.** The two tests under `src/test/java` extend the platform's: the hexagonal rule over
   your distribution, and the modules verified and drawn.

## The loop

```bash
make build         # compile, unit tests, the architecture verified, the jar
make arch-gen      # the hexagon and the module canvases under docs/generated/architecture
make docker-image  # one image: the platform and your story, purse:<version>
make bdd           # your features against that image and the Confluent test cluster
make run ARGS="--purse.owners=Alice:1000"     # from source, against the test cluster
make docker-run ARGS="--purse.owners=Alice:1000"   # the published image
make help          # every target
```

The credentials of the test cluster come from `.env.test.private`, the platform's `.env.private.sample` lists them;
`ENV_DIR` in the Makefile says where the file is. In CI they are in the environment.

## Developing against a platform that is not released yet

From a checkout of the platform, `make platform-install` puts its jars into the local Maven repository under the
checkout's version, where your build takes them first. Pin that version, `0.0.4-SNAPSHOT`, while you work; pin the
release when it is out.

## CI

`.github/workflows/cicd-client-example.yaml` in the platform's repository is the example's pipeline: build, image, features,
publish the image to the registry, on every pull request and push to `main`. A story in a repository of its own
copies it, drops the change gate, and sets its own image name.
