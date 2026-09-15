package dk.mathmagicians.playground.confluent.architecture;

import dk.mathmagicians.playground.architecture.discovery.HexagonDocumenter;
import dk.mathmagicians.playground.confluent.EventingPlatform;
import java.nio.file.Path;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/// The application modules of a distribution, the packages marked `@Module` under the platform's root, verified
/// and documented: C4 diagrams of the modules and their dependencies as PlantUML, a canvas per module, and the
/// hexagon of the modules with their stereotyped types, written under the folder the Gradle task names in
/// `architecture.docs`. The verification runs with the unit tests; the writer carries the `GENERATE` tag and runs
/// from `make arch-gen`, which renders the diagrams. A distribution extends this, nothing more.
public abstract class ModuleDocumentation {

    public static final String GENERATE = "architecture-gen";

    private static final ApplicationModules MODULES =
            ApplicationModules.of(EventingPlatform.class, new Production());

    private static Path output() {
        return Path.of(System.getProperty("architecture.docs", "docs/generated/architecture"), "plantuml");
    }

    @Test
    void modulesDependOnEachOtherAsDeclared() {
        MODULES.verify();
    }

    @Test
    @Tag(GENERATE)
    void writesTheDocumentation() {
        var output = output();
        new Documenter(MODULES, Documenter.Options.defaults().withOutputFolder(output.toString()))
                .writeDocumentation();
        new HexagonDocumenter(MODULES).writeTo(output.resolve("hexagon.puml"));
    }
}
