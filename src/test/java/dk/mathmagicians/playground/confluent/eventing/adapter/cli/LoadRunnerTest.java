package dk.mathmagicians.playground.confluent.eventing.adapter.cli;

import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.APP;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.publisher;
import static org.assertj.core.api.Assertions.assertThat;

import dk.mathmagicians.playground.confluent.eventing.application.GenerateLoad;
import dk.mathmagicians.playground.confluent.eventing.application.PublishMessage;
import dk.mathmagicians.playground.confluent.eventing.domain.Envelope;
import dk.mathmagicians.playground.confluent.eventing.domain.Offer;
import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;

class LoadRunnerTest {

    /// One thread for one second, an offer every ten milliseconds.
    private static final LoadProperties OFFERS =
            new LoadProperties(LoadProperties.Type.OFFER, 1, 10, "APAC", Duration.ofSeconds(1));

    @Test
    void publishesThePayloadTypeOfTheRun() {
        var published = new ConcurrentLinkedQueue<Envelope>();
        var clock = Clock.systemUTC();
        var publishMessage =
                new PublishMessage(OFFERS.region(), APP, publisher(published), clock, ThreadLocalRandom::current);
        var runner = new LoadRunner(OFFERS, new GenerateLoad(publishMessage, clock));

        runner.run(new DefaultApplicationArguments());

        assertThat(published).isNotEmpty().allSatisfy(envelope ->
                assertThat(envelope.payload()).isInstanceOf(Offer.class));
    }
}
