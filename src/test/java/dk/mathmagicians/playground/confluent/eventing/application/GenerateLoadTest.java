package dk.mathmagicians.playground.confluent.eventing.application;

import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.APP;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.REGION;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.publisher;
import static org.assertj.core.api.Assertions.assertThat;

import dk.mathmagicians.playground.confluent.eventing.domain.Envelope;
import dk.mathmagicians.playground.confluent.eventing.domain.Order;
import dk.mathmagicians.playground.confluent.eventing.domain.Payload;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Test;

class GenerateLoadTest {

    private static final int INTERVAL = 10;
    private static final Duration TTL = Duration.ofMillis(100);
    /// Scheduling slack on top of the last interval, for a loaded CI runner.
    private static final Duration SLACK = Duration.ofMillis(300);

    /// Envelopes as the producers publish them.
    private final ConcurrentLinkedQueue<Envelope> published = new ConcurrentLinkedQueue<>();
    private final GenerateLoad useCase = new GenerateLoad(
            new PublishMessage(REGION, APP, publisher(published), Clock.systemUTC(), ThreadLocalRandom::current),
            Clock.systemUTC());

    @Test
    void returnsTheCountThePublisherReceived() {
        var produced = useCase.run(Order::random, 2, INTERVAL, TTL);

        assertThat(produced).isEqualTo(published.size()).isPositive();
    }

    @Test
    void publishesPayloadsFromTheRecipe() {
        useCase.run(Order::random, 1, INTERVAL, TTL);

        assertThat(published).isNotEmpty().allSatisfy(envelope ->
                assertThat(envelope.payload()).isInstanceOf(Order.class));
    }

    @Test
    void stopsAtTheDeadline() {
        var before = Instant.now();

        useCase.run(Order::random, 1, INTERVAL, TTL);

        assertThat(Duration.between(before, Instant.now())).isBetween(TTL, TTL.plusMillis(INTERVAL).plus(SLACK));
    }

    @Test
    void runsEveryThread() {
        var threads = ConcurrentHashMap.<Long>newKeySet();
        GenerateLoad.Recipe<Payload> onThisThread = (random, at) -> {
            threads.add(Thread.currentThread().threadId());
            return Order.random(random, at);
        };

        useCase.run(onThisThread, 3, INTERVAL, TTL);

        assertThat(threads).hasSize(3);
    }

    @Test
    void givesEveryEnvelopeItsOwnId() {
        useCase.run(Order::random, 2, INTERVAL, TTL);

        assertThat(published).extracting(Envelope::id).doesNotHaveDuplicates();
    }
}
