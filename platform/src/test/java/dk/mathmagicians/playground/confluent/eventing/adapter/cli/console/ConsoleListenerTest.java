package dk.mathmagicians.playground.confluent.eventing.adapter.cli.console;

import static dk.mathmagicians.playground.confluent.eventing.application.StoryFixtures.acting;
import static dk.mathmagicians.playground.confluent.eventing.application.StoryFixtures.publishing;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.ALICE;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.APP;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.AT;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.MAD_HATTER;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.REGION;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.TOP_HAT;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.dice;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import dk.mathmagicians.playground.confluent.eventing.application.Story;
import dk.mathmagicians.playground.confluent.eventing.domain.Envelope;
import dk.mathmagicians.playground.confluent.eventing.domain.Offer;
import dk.mathmagicians.playground.confluent.eventing.domain.Order;
import dk.mathmagicians.playground.confluent.eventing.domain.Payload;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/// The console over a string as its input: what a session types is what the story gets, as envelopes.
class ConsoleListenerTest {

    private static final Duration SOON = Duration.ofSeconds(2);
    /// The record is what the text says, so the lines type the creation time the fixtures carry.
    private static final String CREATED_AT =
            "created_at { seconds: " + AT.getEpochSecond() + " nanos: " + AT.getNano() + " }";
    private static final String AN_OFFER = "Offer offer_id: \"OFF-1\" product_id: \"" + TOP_HAT
            + "\" price: 12.5 seller_id: \"" + MAD_HATTER + "\" " + CREATED_AT;
    private static final String AN_ORDER =
            "Order id: \"ORD-1\" customer_id: \"" + ALICE + "\" product_id: \"" + TOP_HAT + "\" " + CREATED_AT;

    /// A story at the table that keeps every envelope it gets.
    private record Listening(String name, List<Envelope> heard) implements Story {

        @Override
        public Set<Class<? extends Payload>> listensTo() {
            return Set.of(Offer.class, Order.class);
        }

        @Override
        public void on(Envelope envelope) {
            heard.add(envelope);
        }

        @Override
        public Optional<String> region() {
            return Optional.of("APAC");
        }
    }

    private final Listening story = new Listening("table", new CopyOnWriteArrayList<>());
    private final ByteArrayOutputStream screen = new ByteArrayOutputStream();
    private final List<Envelope> published = new CopyOnWriteArrayList<>();

    /// A console reading the lines given, ended by EOF.
    private ConsoleListener console(Story story, String... lines) {
        var typed = String.join("\n", lines) + "\n";
        return new ConsoleListener(
                story,
                publishing(published, () -> dice()),
                new ByteArrayInputStream(typed.getBytes(StandardCharsets.UTF_8)),
                new PrintStream(screen, true, StandardCharsets.UTF_8));
    }

    private String screen() {
        return screen.toString(StandardCharsets.UTF_8);
    }

    private void readEverything(ConsoleListener console) {
        console.start();
        await().atMost(SOON).until(() -> !console.isRunning());
    }

    @Test
    void handsATypedMessageToTheStoryAsAnEnvelopeOfThePlatformsRegion() {
        readEverything(console(story, AN_OFFER));

        assertThat(story.heard()).singleElement().satisfies(envelope -> {
            assertThat(envelope.payload()).isEqualTo(new Offer("OFF-1", TOP_HAT, 12.5, MAD_HATTER, AT));
            assertThat(envelope.region()).isEqualTo(REGION);
            assertThat(envelope.app()).isEqualTo(APP);
            assertThat(envelope.at()).isEqualTo(AT);
            assertThat(envelope.id()).isEqualTo(Envelope.id(dice()));
        });
    }

    @Test
    void stampsTheSessionsRegionOnEveryMessageAfterItIsSet() {
        readEverything(console(story, AN_OFFER, "region: AMER", AN_ORDER, AN_OFFER));

        assertThat(story.heard()).extracting(Envelope::region).containsExactly(REGION, "AMER", "AMER");
        assertThat(story.heard().get(1).payload()).isEqualTo(new Order("ORD-1", ALICE, TOP_HAT, AT));
    }

    @Test
    void keepsTheRegionWhenTheNewOneHasNoName() {
        readEverything(console(story, "region: AMER", "region:   ", AN_ORDER));

        assertThat(story.heard()).extracting(Envelope::region).containsExactly("AMER");
        assertThat(screen()).contains("it stays AMER");
    }

    @Test
    void printsTheHelpAtTheStartWithATemplatePerTypeTheStoryListensTo() {
        readEverything(console(story));

        assertThat(screen())
                .contains("help")
                .contains("region:")
                .contains("Offer offer_id: \"\" product_id: \"\" price: 0 seller_id: \"\"")
                .contains("Order id: \"\" customer_id: \"\" product_id: \"\"")
                .doesNotContain("Product product");
    }

    @Test
    void printsTheHelpOnRequest() {
        readEverything(console(story, "help"));

        assertThat(screen().split("Offer offer_id", -1)).hasSize(3);
    }

    @Test
    void refusesALineItCannotReadWithTheReasonAndTheHelpAndTheStoryNeverSeesIt() {
        readEverything(console(story, "Teapot spout: \"long\"", "Offer prize: 12.5", AN_ORDER));

        assertThat(story.heard()).singleElement().satisfies(envelope ->
                assertThat(envelope.payload()).isInstanceOf(Order.class));
        assertThat(screen())
                .contains("Unrecognized type: Teapot")
                .contains("prize");
        assertThat(screen().split("Order id: \"\"", -1)).hasSize(4);
    }

    @Test
    void skipsBlankLines() {
        readEverything(console(story, "", "   ", AN_ORDER));

        assertThat(story.heard()).hasSize(1);
    }

    @Test
    void givesAStoryThatListensToNothingNoConsole() {
        var console = console(acting("load", null), AN_OFFER);

        console.start();

        assertThat(console.isRunning()).isFalse();
        assertThat(screen()).isEmpty();
    }

    @Test
    void stopClosesTheInputAndEndsTheReading() throws Exception {
        var open = new java.io.PipedInputStream();
        var console = new ConsoleListener(
                story,
                publishing(published, () -> dice()),
                open,
                new PrintStream(screen, true, StandardCharsets.UTF_8));
        console.start();
        assertThat(console.isRunning()).isTrue();

        console.stop();

        await().atMost(SOON).until(() -> !console.isRunning());
    }

    /// What a line is, from its text alone: the keywords in any case, the region's name as typed, and anything
    /// else a message of its first word, with an empty body when that is all there is.
    @Nested
    class Lines {

        @Test
        void aBlankLineIsBlank() {
            assertThat(ConsoleListener.Line.of("   ")).isEqualTo(new ConsoleListener.Line.Blank());
        }

        @Test
        void helpInAnyCaseIsHelp() {
            assertThat(ConsoleListener.Line.of("HELP")).isEqualTo(new ConsoleListener.Line.Help());
        }

        @Test
        void qAndQuitAreQuit() {
            assertThat(ConsoleListener.Line.of("q")).isEqualTo(new ConsoleListener.Line.Quit());
            assertThat(ConsoleListener.Line.of("Quit")).isEqualTo(new ConsoleListener.Line.Quit());
        }

        @Test
        void aRegionLineCarriesItsNameAsTyped() {
            assertThat(ConsoleListener.Line.of("region: amer")).isEqualTo(new ConsoleListener.Line.Region("amer"));
            assertThat(ConsoleListener.Line.of("region:")).isEqualTo(new ConsoleListener.Line.Region(""));
        }

        @Test
        void anythingElseIsAMessageOfItsFirstWord() {
            assertThat(ConsoleListener.Line.of("Offer offer_id: \"OFF-1\""))
                    .isEqualTo(new ConsoleListener.Line.Message("Offer", "offer_id: \"OFF-1\""));
            assertThat(ConsoleListener.Line.of("Offer")).isEqualTo(new ConsoleListener.Line.Message("Offer", ""));
        }

        /// Ties the kinds `of` asks to the sealed set, so a new kind without a place in the order fails here.
        @Test
        void everyKindOfLineHasItsPlaceInTheOrder() {
            assertThat(ConsoleListener.Line.class.getPermittedSubclasses()).hasSize(5);
        }
    }
}
