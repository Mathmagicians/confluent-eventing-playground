package dk.mathmagicians.playground.architecture.discovery;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.importer.ImportOption;
import dk.mathmagicians.playground.architecture.discovery.HexagonDocumenter.Options;
import dk.mathmagicians.playground.architecture.discovery.fixture.FixtureApplication;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/// Over the fixture application: four modules, `cli` with a driving adapter, `application` with two ports, `domain`
/// with value objects, a sealed one among them, and a factory, `kafka` with a driven adapter. The fixture lives
/// among the tests, which Modulith leaves out by default, so the import option includes them.
class HexagonDocumenterTest {

    private static final ApplicationModules FIXTURE =
            ApplicationModules.of(FixtureApplication.PACKAGE, ImportOption.Predefined.DO_NOT_INCLUDE_JARS);

    private final String uml = new HexagonDocumenter(FIXTURE).toPlantUml();

    /// A card: the type on the first line, its role in guillemets and a smaller font below.
    private static String card(String type, String role) {
        return "card \"" + type + "\\n<size:10>«" + role + "»</size>\"";
    }

    /// Module names are Modulith's display names, the last package segment capitalised. A column is a frame
    /// without a line, its title the only thing seen; a module is a hexagon, padded by such a frame.
    @Test
    void placesModulesByTheStereotypesOfTheirTypes() {
        assertThat(uml)
                .contains("rectangle \"DRIVING ADAPTERS\" as driving #line:transparent {")
                .contains("hexagon \"Cli\" as m_Cli {")
                .contains("rectangle \"<size:1> </size>\" as m_Cli_in #line:transparent {")
                .contains("rectangle \"APPLICATION\" as core #line:transparent {")
                .contains("hexagon \"Application\" as m_Application {")
                .contains("hexagon \"Domain\" as m_Domain {")
                .contains("rectangle \"DRIVEN ADAPTERS\" as driven #line:transparent {")
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

    @Test
    void listsHexagonalTypesWithTheirRole() {
        assertThat(uml)
                .contains(card("Runner", "PrimaryAdapter"))
                .contains(card("Publish", "PrimaryPort"))
                .contains(card("Publisher", "SecondaryPort"))
                .contains(card("KafkaPublisher", "SecondaryAdapter"));
    }

    @Test
    void listsEveryOtherStereotypedTypeByDefault() {
        assertThat(uml)
                .contains(card("Things", "Factory"))
                .contains(card("Item0", "ValueObject"))
                .contains(card("Item9", "ValueObject"))
                .contains(card("Thing", "ValueObject"))
                .doesNotContain("total)");
    }

    /// Thirteen value objects at the top level, the sealed one counted once with its permitted types: with a
    /// maximum of ten, `Item0` to `Item8` by name, then the ellipsis with the total.
    @Test
    void listsUpToTheMaximumThenTheTotal() {
        var capped = new HexagonDocumenter(FIXTURE, Options.defaults().withMaxTypes(10)).toPlantUml();

        assertThat(capped)
                .contains(card("Item0", "ValueObject"))
                .contains(card("Item8", "ValueObject"))
                .contains(card("... (13 total)", "ValueObject"))
                .doesNotContain(card("Item9", "ValueObject"))
                .doesNotContain(card("Thing", "ValueObject"))
                .doesNotContain(card("Circle", "ValueObject"));
    }

    /// `Shape` permits `Circle` and `Square`: their cards come in a row under the domain's grid, hung from `Shape`
    /// alone, their lines meet at a junction and one arrow goes from there to `Shape`, so the arrows have one head.
    @Test
    void drawsThePermittedTypesUnderTheGridImplementingTheSealedType() {
        assertThat(uml)
                .contains(card("Shape", "ValueObject"))
                .contains("label \"<size:1> </size>\" as m_Domain_Shape_j")
                .contains("m_Domain_Circle .up. m_Domain_Shape_j")
                .contains("m_Domain_Square .up. m_Domain_Shape_j")
                .contains("m_Domain_Shape_j .up.|> m_Domain_Shape")
                .contains("m_Domain_Circle -[hidden]right-> m_Domain_Square")
                .contains("m_Domain_Shape -[hidden]down-> m_Domain_Circle")
                .doesNotContain("m_Domain_Thing -[hidden]down-> m_Domain_Circle");
    }

    /// The domain's cards near square, fourteen in four columns; the application's two ports in one row above
    /// the domain, each hung to the middle of the domain's first row.
    @Test
    void laysCardsOutInAGrid() {
        assertThat(uml)
                .contains("m_Domain_Item0 -[hidden]right-> m_Domain_Item1")
                .contains("m_Domain_Things -[hidden]down-> m_Domain_Item3")
                .contains("m_Application_Publish -[hidden]right-> m_Application_Publisher")
                .contains("m_Application_Publish -[hidden]down-> m_Domain_Item0")
                .contains("m_Application_Publisher -[hidden]down-> m_Domain_Item0");
    }

    /// `Runner` holds a `Publish`, a type reference; `KafkaPublisher` implements `Publisher`, the port; `Publish`
    /// holds a `Publisher` and takes a `Thing`. Both ends have cards, so the arrows join the cards, without a
    /// label: the legend draws each arrow with its relation, in a row hung below the driven adapters and the
    /// domain, and the text legend names the system.
    @Test
    void drawsTheArrowsWithTheLegendAndNoLabels() {
        assertThat(uml)
                .contains("m_Cli_Runner .right.> m_Application_Publish\n")
                .contains("m_Kafka_KafkaPublisher .left.|> m_Application_Publisher\n")
                .contains("m_Application_Publish -right-> m_Application_Publisher\n")
                .contains("m_Application_Publish ..> m_Domain_Thing\n")
                .contains("rectangle \"<size:10>legend</size>\" as legend #line:BBBBBB;text:666666 {")
                .contains("l_implements_from .right.|> l_implements_to : implements")
                .contains("l_uses_to -[hidden]right-> l_implements_from")
                .contains("l_implements_to -[hidden]right-> l_depends_on_from")
                .doesNotContain("speaks")
                .doesNotContain("-[dotted]->")
                .contains("m_Kafka_KafkaPublisher -[hidden]down-> l_uses_from")
                .contains("m_Domain_Square -[hidden]down-> l_uses_from")
                .contains("legend right\n  fixture\nendlegend");
    }

    @Test
    void writesTheSameTextTwice() {
        assertThat(new HexagonDocumenter(FIXTURE).toPlantUml()).isEqualTo(uml);
    }
}
