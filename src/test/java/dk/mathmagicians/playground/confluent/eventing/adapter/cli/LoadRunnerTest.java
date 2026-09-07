package dk.mathmagicians.playground.confluent.eventing.adapter.cli;

import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.APP;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.publisher;
import static org.assertj.core.api.Assertions.assertThat;

import dk.mathmagicians.playground.confluent.eventing.application.GenerateLoadService;
import dk.mathmagicians.playground.confluent.eventing.domain.Envelope;
import dk.mathmagicians.playground.confluent.eventing.domain.Offer;
import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;

class LoadRunnerTest {

    /// One thread for one second, an offer every ten milliseconds.
    private static final LoadProperties OFFERS =
            new LoadProperties(LoadProperties.Type.OFFER, 1, 10, "APAC", Duration.ofSeconds(1));

    @Test
    void publishesThePayloadTypeOfTheRun() {
        var published = new ConcurrentLinkedQueue<Envelope>();
        var generateLoad =
                new GenerateLoadService(OFFERS.region(), APP, publisher(published)::publish, Clock.systemUTC());
        var runner = new LoadRunner(OFFERS, generateLoad);

        runner.run(new DefaultApplicationArguments());

        assertThat(published).isNotEmpty().allSatisfy(envelope ->
                assertThat(envelope.payload()).isInstanceOf(Offer.class));
    }
}
