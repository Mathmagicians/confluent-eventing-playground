package dk.mathmagicians.playground.confluent.eventing.domain;

import java.time.Instant;

public record Offer(String offerId, String productId, double price, String sellerId, Instant createdAt) {
}
