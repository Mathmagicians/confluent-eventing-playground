package dk.mathmagicians.playground.confluent.eventing.domain;

import java.time.Instant;
import java.util.List;

public record Order(String id, String customerId, List<String> products, Instant createdAt) {

    public Order {
        products = List.copyOf(products);
    }
}
