package dk.mathmagicians.playground.confluent.eventing.domain;

import java.time.Instant;
import java.util.random.RandomGenerator;

public record Order(String id, String customerId, String productId, Instant createdAt) implements Payload {

    static final String ID_PREFIX = "ORD";

    /// An order from a character for one of the things.
    public static Order random(RandomGenerator random, Instant at) {
        return new Order(
                Payload.id(ID_PREFIX, random),
                Wonderland.CHARACTERS.next(random),
                Product.id(Wonderland.THINGS.next(random)),
                at);
    }
}
