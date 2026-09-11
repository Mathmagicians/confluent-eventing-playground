package dk.mathmagicians.playground.confluent.eventing.domain;

import org.jmolecules.ddd.annotation.ValueObject;

import java.time.Instant;
import java.util.random.RandomGenerator;

@ValueObject
public record Transaction(
        String transactionId,
        Order orderRef,
        Offer offerRef,
        String customerId,
        String sellerId,
        double price,
        Instant createdAt) implements Payload {

    static final String ID_PREFIX = "TX";

    /// A settlement: the order's customer pays the offer's seller the offer's price, for the thing both name.
    public static Transaction settle(Order order, Offer offer, RandomGenerator random, Instant at) {
        if (!order.productId().equals(offer.productId())) {
            throw new IllegalArgumentException("Cannot settle order and offer for different products: " + order.productId() + " vs " + offer.productId());
        }
        return new Transaction(
                Payload.id(ID_PREFIX, random), order, offer, order.customerId(), offer.sellerId(), offer.price(), at);
    }
}
