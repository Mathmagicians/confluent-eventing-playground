package dk.mathmagicians.playground.confluent.eventing.domain;

import java.time.Instant;
import java.util.random.RandomGenerator;

public record Offer(String offerId, String productId, double price, String sellerId, Instant createdAt)
        implements Payload {

    static final double MIN_PRICE = 0.5;
    static final double MAX_PRICE = 100.0;

    /// An offer for a product from the bounded id space, seller a character, price in whole cents between the bounds.
    public static Offer random(RandomGenerator random, Instant at) {
        throw new UnsupportedOperationException("not implemented");
    }
}
