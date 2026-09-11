package dk.mathmagicians.playground.confluent.eventing.application;

import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.envelope;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import org.junit.jupiter.api.Test;

/// The defaults of the contract: a story that says only its name listens to nothing and does nothing on start.
class StoryTest {

    private final Story quiet = () -> "quiet";

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
        assertThat(quiet.ttl()).isZero();
    }

    @Test
    void refusesAMessageWhenItListensToNothing() {
        var envelope = envelope();

        assertThatIllegalStateException()
                .isThrownBy(() -> quiet.on(envelope))
                .withMessageContaining("quiet")
                .withMessageContaining(envelope.id());
    }

    @Test
    void startsQuietlyByDefault() {
        quiet.start();
    }
}
