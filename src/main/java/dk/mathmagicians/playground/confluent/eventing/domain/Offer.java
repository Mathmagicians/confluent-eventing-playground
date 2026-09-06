package dk.mathmagicians.playground.confluent.eventing.domain;

import java.time.Instant;
import java.util.random.RandomGenerator;

public record Offer(String offerId, String productId, double price, String sellerId, Instant createdAt)
        implements Payload {

    static final String ID_PREFIX = "OFF";
    static final double MIN_PRICE = 0.5;
    static final double MAX_PRICE = 100.0;

    /// An offer for one of the things, seller a character, price in whole cents between the bounds.
    public static Offer random(RandomGenerator random, Instant at) {
        var cents = random.nextLong(Math.round(MIN_PRICE * 100), Math.round(MAX_PRICE * 100) + 1);
        return new Offer(
                Payload.id(ID_PREFIX, random),
                Product.id(Wonderland.THINGS.next(random)),
                cents / 100.0,
                Wonderland.CHARACTERS.next(random),
                at);
    }
}
