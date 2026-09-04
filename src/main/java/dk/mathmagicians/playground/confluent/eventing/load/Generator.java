package dk.mathmagicians.playground.confluent.eventing.load;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.random.RandomGenerator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Runs `concurrent` virtual threads, each sleeping `interval` milliseconds then producing one event from the
/// recipe into the sink, until the TTL has passed. Application core: no Spring, no Kafka.
/// Randomness and time are inputs: the recipe is a function of both, the loop owns the thread's generator and the
/// clock.
public final class Generator<T> {

    /// A pure function from a generator and an instant to an generated payload of type T.
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
        var deadline = clock.instant().plus(ttl);
        log.info("Starting {} threads, each sleeping {} ms, until {}", concurrent, interval, deadline);
        var threadFactory = Thread.ofVirtual().name("load-generator-", 0).factory();
        try (var executor = Executors.newThreadPerTaskExecutor(threadFactory)) {
            Callable<Long> looper = () -> loop(deadline, interval);
            var loops = executor.invokeAll(
                    Collections.nCopies(concurrent, looper));
            return loops.stream().mapToLong(Generator::produced).sum();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return 0;
        }
    }

    private static long produced(Future<Long> someLoop) {
        return switch (someLoop.state()) {
            case SUCCESS -> someLoop.resultNow();
            case FAILED -> throw new IllegalStateException("A loop failed", someLoop.exceptionNow());
            case CANCELLED, RUNNING -> throw new IllegalStateException("A loop is " + someLoop.state());
        };
    }

    /// One thread: its own `ThreadLocalRandom`, sleep, produce, until the deadline. An interrupt ends it with its
    /// count.
    private long loop(Instant deadline, int interval) {
        final String FAILURE_MESSAGE = "Thread %s failed after producing %d events";
        long produced = 0;
        try {
            var random = ThreadLocalRandom.current();
            while (clock.instant().isBefore(deadline)) {
                Thread.sleep(interval);
                var at = clock.instant();
                var payload = recipe.from(random, at);
                sink.accept(payload);
                produced++;
            }
            return produced;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.info("Thread interrupted, produced {} events", produced);
            return produced;
        } catch (RuntimeException e) {
            log.error("Thread failed in loop after producing {} events", produced, e);
            throw new IllegalStateException( FAILURE_MESSAGE.formatted(Thread.currentThread().getName(), produced), e);
        }
    }
}
