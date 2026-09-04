package dk.mathmagicians.playground.confluent.eventing.domain;

import java.time.Instant;
import java.util.random.RandomGenerator;

public record Product(
        String producerId,
        String productId,
        String productName,
        String productDescription,
        Instant createdAt) implements Payload {

    /// Size of the product id space. Product is the partition key, a small space makes keys collide.
    static final int PRODUCTS = 12;

    /// The id of product `n`, the one form every record that references a product uses.
    static String id(int n) {
        throw new UnsupportedOperationException("not implemented");
    }

    /// A product id drawn from the bounded space.
    static String randomId(RandomGenerator random) {
        throw new UnsupportedOperationException("not implemented");
    }

    /// A product from the vocabulary: producer a character, name a thing, description a quote.
    public static Product random(RandomGenerator random, Instant at) {
        throw new UnsupportedOperationException("not implemented");
    }
}
