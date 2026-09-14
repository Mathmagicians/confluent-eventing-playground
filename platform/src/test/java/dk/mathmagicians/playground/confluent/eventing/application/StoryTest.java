package dk.mathmagicians.playground.confluent.eventing.application;

import static dk.mathmagicians.playground.confluent.eventing.application.StoryFixtures.named;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.envelope;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

/// The defaults of the contract: a story that says only its name listens to nothing, reads under its name, plays
/// until stopped, and does nothing on start. And the two inputs the contract reads, the ttl and the name.
class StoryTest {

    private final Story quiet = named("quiet");

    @Test
    void listensToNothingByDefault() {
        assertThat(quiet.listensTo()).isEmpty();
    }

    @Test
    void readsUnderItsNameByDefault() {
        assertThat(quiet.group()).isEqualTo("quiet");
    }

    @Test
    void playsUntilStoppedByDefault() {
        assertThat(quiet.playsFor()).isEmpty();
    }

    @Test
    void isFromNoRegionOfItsOwnByDefault() {
        assertThat(quiet.region()).isEmpty();
    }

    @Test
    void refusesWhatItDoesNotListenTo() {
        var envelope = envelope();

        assertThatIllegalStateException()
                .isThrownBy(() -> quiet.on(envelope))
                .withMessageContaining("quiet does not listen to")
                .withMessageContaining(envelope.payload().toString());
    }

    @Test
    void startsQuietlyByDefault() {
        quiet.start();
    }

    @Test
    void readsATtlAsPositiveSecondsOrLeftEmpty() {
        assertThat(Story.ttl("quiet", null)).isEmpty();
        assertThat(Story.ttl("quiet", " ")).isEmpty();
        assertThat(Story.ttl("quiet", "30")).hasValue(Duration.ofSeconds(30));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Story.ttl("quiet", "0"))
                .withMessageContaining("quiet.ttl")
                .withMessageContaining("left empty for until stopped");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Story.ttl("quiet", "soon"))
                .withMessageContaining("'soon'");
    }

    @Test
    void picksTheStoryByName() {
        var story = Story.named("tea-party", List.of(named("load"), named("tea-party")));

        assertThat(story.name()).isEqualTo("tea-party");
    }

    @Test
    void rejectsAnUnknownNameNamingTheKnownOnes() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Story.named("purse", List.of(named("load"), named("tea-party"))))
                .withMessageContaining("purse")
                .withMessageContaining("[load, tea-party]");
    }

    @Test
    void rejectsTwoStoriesOfOneName() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Story.named("load", List.of(named("load"), named("load"))))
                .withMessageContaining("two stories are named load");
    }
}
