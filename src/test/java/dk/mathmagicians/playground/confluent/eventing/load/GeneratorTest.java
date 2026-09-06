package dk.mathmagicians.playground.confluent.eventing.load;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.junit.jupiter.api.Test;

class GeneratorTest {

    private static final int INTERVAL = 10;
    private static final Duration TTL = Duration.ofMillis(100);
    /// Scheduling slack on top of the last interval, for a loaded CI runner.
    private static final Duration SLACK = Duration.ofMillis(300);

    /// The instant the loop handed in.
    private static final Generator.Recipe<Instant> AT = (_, at) -> at;
    /// The thread the loop runs on.
    private static final Generator.Recipe<Long> THREAD = (_, _) -> Thread.currentThread().threadId();

    @Test
    void returnsTheCountTheSinkReceived() {
        var sink = new ConcurrentLinkedQueue<Instant>();
        var generator = new Generator<>(AT, sink::add, Clock.systemUTC());

        var produced = generator.start(2, INTERVAL, TTL);

        assertThat(produced).isEqualTo(sink.size()).isPositive();
    }

    @Test
    void stopsAtTheDeadline() {
        var generator = new Generator<>(AT, _ -> {}, Clock.systemUTC());
        var before = Instant.now();

        generator.start(1, INTERVAL, TTL);

        assertThat(Duration.between(before, Instant.now())).isBetween(TTL, TTL.plusMillis(INTERVAL).plus(SLACK));
    }

    @Test
    void runsEveryThread() {
        var threads = ConcurrentHashMap.<Long>newKeySet();
        var generator = new Generator<>(THREAD, threads::add, Clock.systemUTC());

        generator.start(3, INTERVAL, TTL);

        assertThat(threads).hasSize(3);
    }
}
