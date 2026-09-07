package dk.mathmagicians.playground.confluent.eventing.application;

import dk.mathmagicians.playground.confluent.eventing.domain.Envelope;
import dk.mathmagicians.playground.confluent.eventing.domain.Payload;
import dk.mathmagicians.playground.confluent.eventing.domain.Receipt;
import java.time.Clock;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;
import org.jmolecules.architecture.hexagonal.PrimaryPort;

/// Use case, the feature's words: a generator in a region publishes a payload in an envelope and receives where the
/// message landed. Randomness and time are inputs: the id comes from the supplied generator, the instant from the
/// clock.
@PrimaryPort
public record PublishMessage(
        String region,
        String app,
        Publisher publisher,
        Clock clock,
        Supplier<RandomGenerator> random) {

    /// Puts the payload in an envelope stamped with an id, the region, the app, and the instant, hands it to the
    /// publisher, and answers its receipt.
    public CompletableFuture<Receipt> publish(Payload payload) {
        var envelope = Envelope.of(random.get(), region, app, clock.instant(), payload);
        return publisher.publish(envelope);
    }
}
