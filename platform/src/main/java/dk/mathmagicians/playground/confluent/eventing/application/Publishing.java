package dk.mathmagicians.playground.confluent.eventing.application;

import dk.mathmagicians.playground.confluent.eventing.domain.Envelope;
import dk.mathmagicians.playground.confluent.eventing.domain.Payload;
import java.time.Clock;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;
import org.jspecify.annotations.Nullable;

/// What every story publishes with, wired once by the platform: the application's name, the region the platform
/// hands down, the profile's publisher, the clock, and the random source. A story asks for the use case of its
/// region, or of the platform's when it has none of its own.
public record Publishing(
        String app, String region, Publisher publisher, Clock clock, Supplier<RandomGenerator> random) {

    /// The use case of the region given, or of the platform's region when none is.
    public PublishMessage from(@Nullable String region) {
        return new PublishMessage(region == null ? this.region : region, app, publisher, clock, random);
    }

    /// An envelope as a publisher of the region would stamp it: a drawn id, the application's name, the instant.
    public Envelope envelope(String region, Payload payload) {
        return Envelope.of(random.get(), region, app, clock.instant(), payload);
    }
}
