package dk.mathmagicians.playground.confluent.stories.load;

import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.APP;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.REGION;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.publisher;
import static org.assertj.core.api.Assertions.assertThat;

import dk.mathmagicians.playground.confluent.eventing.application.PublishMessage;
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
    private final Clock clock = Clock.systemUTC();
    private final PublishMessage publishMessage =
            new PublishMessage(REGION, APP, publisher(published), clock, ThreadLocalRandom::current);

    private GenerateLoad load(GenerateLoad.Recipe<? extends Payload> payloads, int concurrent) {
        return new GenerateLoad(publishMessage, clock, payloads, concurrent, INTERVAL, TTL);
    }

    @Test
    void isTheLoadStory() {
        assertThat(load(Order::random, 1).name()).isEqualTo("load");
        assertThat(load(Order::random, 1).listensTo()).isEmpty();
    }

    @Test
    void returnsTheCountThePublisherReceived() {
        var produced = load(Order::random, 2).run();

        assertThat(produced).isEqualTo(published.size()).isPositive();
    }

    @Test
    void publishesPayloadsFromTheRecipe() {
        load(Order::random, 1).run();

        assertThat(published).isNotEmpty().allSatisfy(envelope ->
                assertThat(envelope.payload()).isInstanceOf(Order.class));
    }

    @Test
    void startRunsTheLoad() {
        load(Order::random, 1).start();

        assertThat(published).isNotEmpty();
    }

    @Test
    void stopsAtTheDeadline() {
        var before = Instant.now();

        load(Order::random, 1).run();

        assertThat(Duration.between(before, Instant.now())).isBetween(TTL, TTL.plusMillis(INTERVAL).plus(SLACK));
    }

    @Test
    void runsEveryThread() {
        var threads = ConcurrentHashMap.<Long>newKeySet();
        GenerateLoad.Recipe<Payload> onThisThread = (random, at) -> {
            threads.add(Thread.currentThread().threadId());
            return Order.random(random, at);
        };

        load(onThisThread, 3).run();

        assertThat(threads).hasSize(3);
    }

    @Test
    void givesEveryEnvelopeItsOwnId() {
        load(Order::random, 2).run();

        assertThat(published).extracting(Envelope::id).doesNotHaveDuplicates();
    }
}
