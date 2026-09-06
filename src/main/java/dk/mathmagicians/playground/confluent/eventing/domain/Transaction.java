package dk.mathmagicians.playground.confluent.eventing.domain;

import java.time.Instant;
import java.util.random.RandomGenerator;

public record Transaction(
        String transactionId,
        Order orderRef,
        Offer offerRef,
        String customerId,
        String sellerId,
        double price,
        Instant createdAt) implements Payload {

    static final String ID_PREFIX = "TX";

    /// A transaction composed from `Order.random` and `Offer.random`, customer, seller, and price copied from them.
    public static Transaction random(RandomGenerator random, Instant at) {
        var order = Order.random(random, at);
        var offer = Offer.random(random, at);
        return new Transaction(
                Payload.id(ID_PREFIX, random), order, offer, order.customerId(), offer.sellerId(), offer.price(), at);
    }
}
