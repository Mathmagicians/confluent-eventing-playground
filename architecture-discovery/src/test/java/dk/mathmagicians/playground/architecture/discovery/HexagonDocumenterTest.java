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
                .contains("rectangle \" \" as m_Cli_in #line:transparent {")
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

    /// `Shape` permits `Circle` and `Square`: their cards follow the sealed type's, their lines meet at a junction
    /// and one arrow goes from there to `Shape`, so the arrows have one head.
    @Test
    void drawsThePermittedTypesAfterTheSealedTypeImplementingIt() {
        var shape = uml.indexOf(card("Shape", "ValueObject"));
        var circle = uml.indexOf(card("Circle", "ValueObject"));
        var square = uml.indexOf(card("Square", "ValueObject"));

        assertThat(shape).isPositive();
        assertThat(circle).isGreaterThan(shape);
        assertThat(square).isGreaterThan(circle);
        assertThat(uml)
                .contains("label \"<size:1> </size>\" as m_Domain_Shape_j")
                .contains("m_Domain_Circle .up. m_Domain_Shape_j")
                .contains("m_Domain_Square .up. m_Domain_Shape_j")
                .contains("m_Domain_Shape_j .up.|> m_Domain_Shape");
    }

    /// The domain's cards near square, sixteen in four columns; the application's two ports around the domain,
    /// one above, hung to the domain's first card, one below, hung from the domain's bottom row.
    @Test
    void laysCardsOutInAGrid() {
        assertThat(uml)
                .contains("m_Domain_Item0 -[hidden]right-> m_Domain_Item1")
                .contains("m_Domain_Things -[hidden]down-> m_Domain_Item3")
                .contains("m_Application_Publish -[hidden]down-> m_Domain_Things")
                .contains("m_Domain_Shape -[hidden]down-> m_Application_Publisher")
                .contains("m_Domain_Thing -[hidden]down-> m_Application_Publisher")
                .doesNotContain("m_Application_Publish -[hidden]right-> m_Application_Publisher");
    }

    /// `Runner` holds a `Publish`, a type reference; `KafkaPublisher` implements `Publisher`, the port. Both ends
    /// have cards, so the arrows join the cards, without a label: the legend draws each arrow with its relation, in
    /// a row hung below the driven adapters and the application, and the text legend names the system.
    @Test
    void drawsTheArrowsWithTheLegendAndNoLabels() {
        assertThat(uml)
                .contains("m_Cli_Runner .right.> m_Application_Publish\n")
                .contains("m_Kafka_KafkaPublisher .left.|> m_Application_Publisher\n")
                .contains("rectangle \"<size:10>legend</size>\" as legend #line:BBBBBB;text:666666 {")
                .contains("l_implements_from .right.|> l_implements_to : implements")
                .contains("l_uses_to -[hidden]right-> l_implements_from")
                .contains("l_implements_to -[hidden]right-> l_depends_on_from")
                .doesNotContain("speaks")
                .doesNotContain("-[dotted]->")
                .contains("m_Kafka_KafkaPublisher -[hidden]down-> l_uses_from")
                .contains("m_Application_Publisher -[hidden]down-> l_uses_from")
                .contains("legend right\n  fixture\nendlegend");
    }

    @Test
    void writesTheSameTextTwice() {
        assertThat(new HexagonDocumenter(FIXTURE).toPlantUml()).isEqualTo(uml);
    }
}
