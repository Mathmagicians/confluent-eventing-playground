package dk.mathmagicians.playground.confluent.eventing.domain;

import java.time.Instant;
import java.util.List;
import java.util.random.RandomGenerator;

public record Order(String id, String customerId, List<String> products, Instant createdAt) implements Payload {

    static final int MAX_ITEMS = 5;

    public Order {
        products = List.copyOf(products);
    }

    /// An order from a character for one to `MAX_ITEMS` products, the ids a stream of ints from the bounded space.
    public static Order random(RandomGenerator random, Instant at) {
        throw new UnsupportedOperationException("not implemented");
    }
}
