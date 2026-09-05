package dk.mathmagicians.playground.confluent.eventing.cli;

import static org.assertj.core.api.Assertions.assertThat;

import dk.mathmagicians.playground.confluent.eventing.domain.Envelope;
import dk.mathmagicians.playground.confluent.eventing.domain.Offer;
import dk.mathmagicians.playground.confluent.eventing.load.LoadProperties;
import java.time.Duration;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;

class LoadRunnerTest {

    /// One thread for one second, an offer every ten milliseconds.
    private static final LoadProperties OFFERS =
            new LoadProperties(LoadProperties.Type.OFFER, 1, 10, "APAC", Duration.ofSeconds(1));

    @Test
    void publishesEachPayloadInAnEnvelopeStampedWithRegionAndApp() {
        var published = new ConcurrentLinkedQueue<Envelope>();
        var runner = new LoadRunner(OFFERS, published::add);

        runner.run(new DefaultApplicationArguments());

        assertThat(published).isNotEmpty().allSatisfy(envelope -> {
            assertThat(envelope.region()).isEqualTo(OFFERS.region());
            assertThat(envelope.app()).isEqualTo(LoadRunner.APP);
            assertThat(envelope.payload()).isInstanceOf(Offer.class);
        });
    }

    @Test
    void givesEveryEnvelopeItsOwnId() {
        var published = new ConcurrentLinkedQueue<Envelope>();
        var runner = new LoadRunner(OFFERS, published::add);

        runner.run(new DefaultApplicationArguments());

        assertThat(published).extracting(Envelope::id).doesNotHaveDuplicates();
    }
}
