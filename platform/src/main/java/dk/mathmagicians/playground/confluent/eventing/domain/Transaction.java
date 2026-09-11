package dk.mathmagicians.playground.confluent.eventing.domain;

import org.jmolecules.ddd.annotation.ValueObject;

import java.time.Instant;

@ValueObject
public record Transaction(
        String id,
        Order orderRef,
        Offer offerRef,
        String customerId,
        String sellerId,
        double price,
        Instant createdAt) implements Payload {

    static final String ID_PREFIX = "TX";

    static String id(String orderId, String offerId) {
        return String.join("-", ID_PREFIX, orderId, offerId);
    }
    /// A settlement: the order's customer pays the offer's seller the offer's price for the thing
    public static Transaction settle(Order order, Offer offer, Instant at) {
        if (!order.productId().equals(offer.productId())) {
            throw new IllegalArgumentException("Cannot settle order and offer are for different products: " + order.productId() + " vs " + offer.productId());
        }
        return new Transaction(
            Transaction.id(order.id(), offer.id()), order, offer, order.customerId(), offer.sellerId(), offer.price(), at);
    }
}
