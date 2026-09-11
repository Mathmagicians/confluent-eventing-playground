package dk.mathmagicians.playground.confluent.eventing.application;

import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.APP;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.CLOCK;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.REGION;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.publisher;

import dk.mathmagicians.playground.confluent.eventing.domain.Envelope;
import dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures;
import dk.mathmagicians.playground.confluent.eventing.domain.Payload;
import java.time.Duration;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;
import org.jspecify.annotations.Nullable;

/// Stories for the tests of the substrate, one that says only its name and one with everything spelled out, and
/// what a story publishes with.
public final class StoryFixtures {

    /// A story with every answer given, that counts its starts and takes every message. No ttl is until stopped.
    public record Fake(
            String name,
            String group,
            Set<Class<? extends Payload>> listensTo,
            @Nullable Duration ttl,
            AtomicInteger starts) implements Story {

        @Override
        public Optional<Duration> playsFor() {
            return Optional.ofNullable(ttl);
        }

        @Override
        public void start() {
            starts.incrementAndGet();
        }

        @Override
        public void on(Envelope envelope) {
        }
    }

    /// A story that says only its name: the contract's defaults for the rest.
    public static Story named(String name) {
        return () -> name;
    }

    /// A story that acts on its own, listening to nothing, for the ttl given.
    public static Fake acting(String name, @Nullable Duration ttl) {
        return new Fake(name, name, Set.of(), ttl, new AtomicInteger());
    }

    /// A story that listens to the types given under its own name, for the ttl given.
    public static Fake listening(String name, Set<Class<? extends Payload>> types, @Nullable Duration ttl) {
        return new Fake(name, name, types, ttl, new AtomicInteger());
    }

    /// What a story publishes with in a test: envelopes stamped `APP` at `AT`, kept in `published`, their ids
    /// from the random source given.
    public static Publishing publishing(Collection<Envelope> published, Supplier<RandomGenerator> random) {
        return new Publishing(APP, publisher(published), CLOCK, random);
    }

    /// The use case of `REGION`, every id from a fresh seeded generator, so one publish has one known id.
    public static PublishMessage publishMessage(Collection<Envelope> published) {
        return publishing(published, EventFixtures::dice).from(REGION);
    }

    private StoryFixtures() {
    }
}
