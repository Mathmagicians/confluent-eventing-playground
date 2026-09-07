package dk.mathmagicians.playground.confluent;

import dk.mathmagicians.playground.architecture.discovery.HexagonDocumenter;
import java.nio.file.Path;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/// The application modules, the packages marked `@Module`, verified and documented: C4 diagrams of the modules and
/// their dependencies as PlantUML, a canvas per module, and the hexagon of the modules with their stereotyped types,
/// written to `OUTPUT`. The verification runs with the unit tests; the writer carries the `GENERATE` tag and runs
/// from `make arch-gen`, which renders the diagrams.
class ArchitectureDocumentationTest {

    static final String OUTPUT = "docs/generated/architecture/plantuml";
    static final String GENERATE = "architecture-gen";

    private static final ApplicationModules MODULES =
            ApplicationModules.of(ConfluentLoadGeneratorApplication.class);

    @Test
    void modulesDependOnEachOtherAsDeclared() {
        MODULES.verify();
    }

    @Test
    @Tag(GENERATE)
    void writesTheDocumentation() {
        new Documenter(MODULES, Documenter.Options.defaults().withOutputFolder(OUTPUT)).writeDocumentation();
        new HexagonDocumenter(MODULES).writeTo(Path.of(OUTPUT, "hexagon.puml"));
    }
}
