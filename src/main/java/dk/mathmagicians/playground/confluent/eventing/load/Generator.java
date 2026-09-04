package dk.mathmagicians.playground.confluent.eventing.load;

import java.time.Duration;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Runs `concurrent` virtual threads, each sleeping `interval` milliseconds then producing one event from the
/// supplier, for a region, until the TTL has passed. Application core: no Spring, no Kafka.
/// Stub: `start` logs the request. The threads and the loop are not implemented.
public abstract class Generator<T> {

    private static final Logger log = LoggerFactory.getLogger(Generator.class);

    public final void start(int concurrent, int interval, String region, Duration ttl) {
        log.info("{} for {}: {} concurrent, interval {} ms, {} events per second, {} seconds",
                getClass().getSimpleName(), region, concurrent, interval, concurrent * 1000L / interval, ttl.toSeconds());
    }

    protected abstract Supplier<T> supplier();
}
