package dk.mathmagicians.playground.confluent.eventing.application;

import dk.mathmagicians.playground.confluent.eventing.domain.Envelope;
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
import org.jmolecules.architecture.hexagonal.Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Runs the producers as virtual threads. Each owns its `ThreadLocalRandom` and reads the clock, stamps every
/// payload into an envelope for the region and the app, and publishes it through the use case.
@Application
public record GenerateLoadService(String region, String app, PublishMessage publishMessage, Clock clock)
        implements GenerateLoad {

    private static final Logger log = LoggerFactory.getLogger(GenerateLoadService.class);

    /// Starts the threads, waits for all of them, returns the number of messages published.
    @Override
    public long run(Recipe<? extends Payload> payloads, int concurrent, int interval, Duration ttl) {
        var deadline = clock.instant().plus(ttl);
        log.info("Starting {} threads, each sleeping {} ms, until {}", concurrent, interval, deadline);
        var threadFactory = Thread.ofVirtual().name("generator-", 0).factory();
        try (var executor = Executors.newThreadPerTaskExecutor(threadFactory)) {
            Callable<Long> looper = () -> loop(payloads, deadline, interval);
            List<Future<Long>> loops = executor.invokeAll(Collections.nCopies(concurrent, looper));
            return loops.stream().mapToLong(GenerateLoadService::produced).sum();
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

    /// One thread: sleep, stamp, publish, until the deadline. An interrupt ends it with its count.
    private long loop(Recipe<? extends Payload> payloads, Instant deadline, int interval) {
        long produced = 0;
        try {
            var random = ThreadLocalRandom.current();
            while (clock.instant().isBefore(deadline)) {
                Thread.sleep(interval);
                var at = clock.instant();
                var envelope = Envelope.of(random, region, app, at, payloads.from(random, at));
                publishMessage.publish(envelope);
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
