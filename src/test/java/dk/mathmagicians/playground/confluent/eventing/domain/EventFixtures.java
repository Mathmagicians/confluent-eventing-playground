package dk.mathmagicians.playground.confluent.eventing.domain;

import dk.mathmagicians.playground.confluent.eventing.dto.Envelope;
import java.time.Instant;
import java.util.List;

public final class EventFixtures {

    public static final Instant AT = Instant.parse("2026-09-04T10:15:30.123456789Z");

    public static Envelope<Offer> envelope() {
        return new Envelope<>("e-1", "EMEA", "load-generator", AT, offer());
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

    private EventFixtures() {
    }
}
