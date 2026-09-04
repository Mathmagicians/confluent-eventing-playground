package dk.mathmagicians.playground.confluent.eventing.domain;

import java.time.Instant;

public record Transaction(
        String transactionId,
        Order orderRef,
        Offer offerRef,
        String customerId,
        String sellerId,
        double price,
        Instant createdAt) {
}
