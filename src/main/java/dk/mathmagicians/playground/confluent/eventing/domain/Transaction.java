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

    /// A transaction composed from `Order.random` and `Offer.random`, customer, seller, and price copied from them.
    public static Transaction random(RandomGenerator random, Instant at) {
        throw new UnsupportedOperationException("not implemented");
    }
}
