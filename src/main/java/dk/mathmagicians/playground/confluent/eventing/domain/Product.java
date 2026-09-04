package dk.mathmagicians.playground.confluent.eventing.domain;

import java.time.Instant;

public record Product(
        String producerId,
        String productId,
        String productName,
        String productDescription,
        Instant createdAt) {
}
