package dk.mathmagicians.playground.architecture.discovery;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.importer.ImportOption;
import dk.mathmagicians.playground.architecture.discovery.fixture.FixtureApplication;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/// Over the fixture application: four modules, `cli` with a driving adapter, `application` with two ports, `domain`
/// with twelve value objects and a factory, `kafka` with a driven adapter. The fixture lives among the tests, which
/// Modulith leaves out by default, so the import option includes them.
class HexagonDocumenterTest {

    private static final ApplicationModules FIXTURE =
            ApplicationModules.of(FixtureApplication.PACKAGE, ImportOption.Predefined.DO_NOT_INCLUDE_JARS);

    private final String uml = new HexagonDocumenter(FIXTURE).toPlantUml();

    /// Module names are Modulith's display names, the last package segment capitalised.
    @Test
    void placesModulesByTheStereotypesOfTheirTypes() {
        assertThat(uml)
                .contains("rectangle \"DRIVING ADAPTERS\" as driving {")
                .contains("hexagon \"Cli\" as m_Cli {")
                .contains("rectangle \"APPLICATION\" as core {")
                .contains("hexagon \"Application\" as m_Application {")
                .contains("hexagon \"Domain\" as m_Domain {")
                .contains("rectangle \"DRIVEN ADAPTERS\" as driven {")
                .contains("hexagon \"Kafka\" as m_Kafka {");
    }

    @Test
    void nestsTheDomainInsideTheApplication() {
        var application = uml.indexOf("hexagon \"Application\"");
        var domain = uml.indexOf("hexagon \"Domain\"");
        var driven = uml.indexOf("rectangle \"DRIVEN ADAPTERS\"");

        assertThat(application).isLessThan(domain);
        assertThat(domain).isLessThan(driven);
    }

    /// A card: the type on the first line, its role in guillemets and a smaller font below.
    private static String card(String type, String role) {
        return "card \"" + type + "\\n<size:10>«" + role + "»</size>\"";
    }

    @Test
    void listsHexagonalTypesWithTheirRole() {
        assertThat(uml)
                .contains(card("Runner", "PrimaryAdapter"))
                .contains(card("Publish", "PrimaryPort"))
                .contains(card("Publisher", "SecondaryPort"))
                .contains(card("KafkaPublisher", "SecondaryAdapter"));
    }

    @Test
    void listsOtherStereotypesBelowTheLimit() {
        assertThat(uml).contains(card("Things", "Factory"));
    }

    /// Twelve value objects: `Item0` to `Item8` by name, then the ellipsis with the total.
    @Test
    void listsUpToTheLimitThenTheTotal() {
        assertThat(uml)
                .contains(card("Item0", "ValueObject"))
                .contains(card("Item8", "ValueObject"))
                .contains(card("... (12 total)", "ValueObject"))
                .doesNotContain(card("Item9", "ValueObject"))
                .doesNotContain(card("Thing", "ValueObject"));
    }

    /// `Runner` holds a `Publish`, a type reference; `KafkaPublisher` implements `Publisher`, the port. Both ends
    /// have cards, so the arrows join the cards.
    @Test
    void drawsAdapterDependenciesBetweenTheTypesIntoTheHexagonOnly() {
        assertThat(uml)
                .contains("m_Cli_Runner .right.> m_Application_Publish : depends on")
                .contains("m_Kafka_KafkaPublisher .left.> m_Application_Publisher : implements");
        assertThat(uml.lines()).noneMatch(line -> line.startsWith("m_") && line.contains(" m_Domain"));
    }

    @Test
    void writesTheSameTextTwice() {
        assertThat(new HexagonDocumenter(FIXTURE).toPlantUml()).isEqualTo(uml);
    }
}
