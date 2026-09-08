# Gradle and make

## Make, the one entry point

- Every workflow step is a `make` target, so what runs on GitHub is what runs on a developer machine. `make help`
  is built from the `##` comments, sections from `##@` lines; every target carries one.
- Recipes start with `@`. `make -n <target>` shows a recipe when it matters.
- `WITH_ENV` sources `.env.<ENV>.private` into the one command that needs it, `ENV` defaulting to `test`. A
  variable passed on the command line, `make docker-smoke ENV=prod`, reaches the sub-make.
- `VERSION` is Gradle's, evaluated once on first use through `$(eval ...)`; a target name that needs it would
  evaluate it on every invocation, so targets with a version in their name go through a sub-make or, better, do not
  exist.
- Print targets, `version`, `next-version`, `docker-repo`, give workflows a value without a second copy of how it
  is derived.
- `proto-check` and its siblings compare committed generated output with a fresh run and need the output staged,
  so `make build` is red until `git add src/generated`.

## Gradle owns up-to-date

- `bootBuildImage` has the jar and the image name as inputs and no output, so it always runs. Declaring a build
  record under `build/image/<version>.txt` as its output lets Gradle skip it while nothing changed, content-hashed,
  so a `touch` changes nothing and an edited resource rebuilds. `docker-publish`, `bdd`, and the compose targets
  depend on `docker-image`; a registry image runs as it is.
- `generateProto` is not configuration-cache compatible; only `proto-gen` runs it, every other build keeps the
  cache. Generated code is committed and compiled as a plain source directory.
- A library published as a snapshot by a sibling workflow is consumed as `latest.integration`, with
  `cacheDynamicVersionsFor 0` and `cacheChangingModulesFor 0`, so every build looks again. GitHub Packages needs
  `GITHUB_ACTOR` and `GITHUB_TOKEN` even to read; `mavenLocal` filtered to the group serves the developer loop.
- The Confluent Maven repository, `https://packages.confluent.io/maven/`, serves the Protobuf serializer and the
  registry client; versions are pinned with a date comment, no BOM carries them.
- Spring Boot 4 moved `KafkaAutoConfiguration` and `KafkaProperties` to `org.springframework.boot.kafka.autoconfigure`.

## Images and profiles

- A buildpack image takes `--load.*` arguments after the image name; the profile is `SPRING_PROFILES_ACTIVE`, which
  `docker-run`, the compose targets, and the BDD container all set. Without it the image falls back to `local` and
  writes to the log.
- The one-shot generator exits after its TTL; the minimum load is one producer, one second interval, two seconds,
  defined once in the Makefile as `MINIMUM`.
