package dk.mathmagicians.playground.confluent.eventing.domain;

import java.time.Instant;
import java.util.List;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;

public final class EventFixtures {

    public static final Instant AT = Instant.parse("2026-09-04T10:15:30.123456789Z");

    /// Rule Forty-two, the oldest rule in the book.
    public static final long SEED = 42;

    /// A fresh generator seeded with `SEED`: the same sequence in every test.
    public static RandomGenerator dice() {
        return RandomGeneratorFactory.getDefault().create(SEED);
    }


    public static Product product() {
        return new Product("Mad Hatter", "p-1", "Pocket Watch", "We're all mad here.", AT);
    }

    public static Order order() {
        return new Order("o-1", "Alice", List.of("Pocket Watch", "Tea Set"), AT);
    }

    public static Offer offer() {
        return new Offer("of-1", "p-1", 1.5, "White Rabbit", AT);
    }

    public static Transaction transaction() {
        return new Transaction("t-1", order(), offer(), "Alice", "White Rabbit", 1.5, AT);
    }

    /// One fixture per record `Payload` permits.
    public static List<Payload> payloads() {

        return List.of(product(), offer(), order(), transaction());
    }

    public static Envelope envelope() {
        return envelope(offer());
    }

    public static Envelope envelope(Payload payload) {
        return new Envelope("e-1", "EMEA", "load-generator", AT, payload);
    }

    private EventFixtures() {
    }
}
