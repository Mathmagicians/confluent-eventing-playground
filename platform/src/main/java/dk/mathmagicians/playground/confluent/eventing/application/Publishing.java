package dk.mathmagicians.playground.confluent.eventing.application;

import java.time.Clock;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;

/// What every story publishes with, wired once by the platform: the application's name, the profile's publisher,
/// the clock, and the random source. A story asks for the use case of its region.
public record Publishing(String app, Publisher publisher, Clock clock, Supplier<RandomGenerator> random) {

    public PublishMessage from(String region) {
        return new PublishMessage(region, app, publisher, clock, random);
    }
}
