package dk.mathmagicians.playground.confluent.eventing.application;

import dk.mathmagicians.playground.confluent.eventing.domain.Payload;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;
import org.jmolecules.architecture.hexagonal.PrimaryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Use case: `concurrent` producers, each publishing one payload from the recipe every `interval` milliseconds
/// until the TTL has passed. Producers are virtual threads, each with its own `ThreadLocalRandom`, reading the
/// clock, publishing through `PublishMessage`.
@PrimaryPort
public record GenerateLoad(PublishMessage publishMessage, Clock clock) {

    /// A pure function from a generator and an instant to a payload.
    @FunctionalInterface
    public interface Recipe<T> {
        T from(RandomGenerator random, Instant at);
    }

    private static final Logger log = LoggerFactory.getLogger(GenerateLoad.class);

    /// Starts the producers, waits for all of them, answers the number of messages published.
    public long run(Recipe<? extends Payload> payloads, int concurrent, int interval, Duration ttl) {
        var deadline = clock.instant().plus(ttl);
        log.info("Starting {} threads, each sleeping {} ms, until {}", concurrent, interval, deadline);
        var threadFactory = Thread.ofVirtual().name("generator-", 0).factory();
        try (var executor = Executors.newThreadPerTaskExecutor(threadFactory)) {
            Callable<Long> looper = () -> loop(payloads, deadline, interval);
            List<Future<Long>> loops = executor.invokeAll(Collections.nCopies(concurrent, looper));
            return loops.stream().mapToLong(GenerateLoad::produced).sum();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while waiting for the loops to end, produced count unknown");
            return 0;
        }
    }

    private static long produced(Future<Long> loop) {
        return switch (loop.state()) {
            case SUCCESS -> loop.resultNow();
            case FAILED -> throw new IllegalStateException("A loop failed", loop.exceptionNow());
            case CANCELLED, RUNNING -> throw new IllegalStateException("A loop is " + loop.state());
        };
    }

    /// One producer: sleep, draw, publish, until the deadline. An interrupt ends it with its count.
    private long loop(Recipe<? extends Payload> payloads, Instant deadline, int interval) {
        long produced = 0;
        try {
            var random = ThreadLocalRandom.current();
            while (clock.instant().isBefore(deadline)) {
                Thread.sleep(interval);
                publishMessage.publish(payloads.from(random, clock.instant()));
                produced++;
            }
            return produced;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.info("Thread interrupted, produced {} events", produced);
            return produced;
        } catch (RuntimeException e) {
            log.error("Thread failed in loop after producing {} events", produced, e);
            throw new IllegalStateException(
                    "Thread %s failed after producing %d events".formatted(Thread.currentThread().getName(), produced),
                    e);
        }
    }
}
