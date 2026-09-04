package dk.mathmagicians.playground.confluent.eventing.load;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Consumer;
import java.util.random.RandomGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Runs `concurrent` virtual threads, each sleeping `interval` milliseconds then producing one event from the
/// recipe into the sink, until the TTL has passed. Application core: no Spring, no Kafka.
/// Randomness and time are inputs: the recipe is a function of both, the loop owns the thread's generator and the
/// clock.
public final class Generator<T> {

    /// A pure function from a generator and an instant to an event.
    @FunctionalInterface
    public interface Recipe<T> {
        T from(RandomGenerator random, Instant at);
    }

    private static final Logger log = LoggerFactory.getLogger(Generator.class);

    private final Recipe<T> recipe;
    private final Consumer<? super T> sink;
    private final Clock clock;

    public Generator(Recipe<T> recipe, Consumer<? super T> sink, Clock clock) {
        this.recipe = recipe;
        this.sink = sink;
        this.clock = clock;
    }

    /// Starts the threads, waits for all of them, returns the number of events produced.
    public long start(int concurrent, int interval, Duration ttl) {
        throw new UnsupportedOperationException("not implemented");
    }

    /// One thread: its own `ThreadLocalRandom`, sleep, produce, until the deadline. An interrupt ends it with its
    /// count.
    private long loop(Instant deadline, int interval) {
        throw new UnsupportedOperationException("not implemented");
    }
}
