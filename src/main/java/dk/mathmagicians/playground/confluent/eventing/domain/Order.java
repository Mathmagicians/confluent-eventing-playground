package dk.mathmagicians.playground.confluent.eventing.domain;

import java.time.Instant;
import java.util.List;
import java.util.random.RandomGenerator;
import java.util.stream.IntStream;

public record Order(String id, String customerId, List<String> products, Instant createdAt) implements Payload {

    static final int MAX_ITEMS = 5;

    public Order {
        products = List.copyOf(products);
    }

    /// An order from a character for one to `MAX_ITEMS` things, each drawn on its own so a thing may repeat.
    public static Order random(RandomGenerator random, Instant at) {
        var items = random.nextInt(1, MAX_ITEMS + 1);
        var products = IntStream.range(0, items)
                .mapToObj(_ -> Product.id(Wonderland.THINGS.next(random)))
                .toList();
        return new Order(Payload.id("O", random), Wonderland.CHARACTERS.next(random), products, at);
    }
}
