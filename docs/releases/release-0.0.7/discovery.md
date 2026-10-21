# Discovery

The example map of each feature in the [catalog](release-0.0.7.md): its story, its rules with examples, its
questions, and the decisions it raises for [docs/adr.md](../../adr.md).

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
