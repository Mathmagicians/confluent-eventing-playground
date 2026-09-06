package dk.mathmagicians.playground.confluent.eventing.application;

import dk.mathmagicians.playground.confluent.eventing.domain.Payload;
import java.time.Duration;
import java.time.Instant;
import java.util.random.RandomGenerator;
import org.jmolecules.architecture.hexagonal.PrimaryPort;

/// Driving port: `concurrent` producers, each publishing one payload from the recipe every `interval`
/// milliseconds until the TTL has passed. Answers the number of messages published.
@PrimaryPort
@FunctionalInterface
public interface GenerateLoad {

    /// A pure function from a generator and an instant to a payload.
    @FunctionalInterface
    interface Recipe<T> {
        T from(RandomGenerator random, Instant at);
    }

    long run(Recipe<? extends Payload> payloads, int concurrent, int interval, Duration ttl);
}
