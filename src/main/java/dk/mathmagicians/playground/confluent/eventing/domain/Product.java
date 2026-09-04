package dk.mathmagicians.playground.confluent.eventing.domain;

import java.time.Instant;
import java.util.random.RandomGenerator;

public record Product(
        String producerId,
        String productId,
        String productName,
        String productDescription,
        Instant createdAt) implements Payload {

    /// The id of product `n`, our products are the things, product id is 4 chars uppercase with space removed
    static String id(String s) {
        var base = s.replaceAll("\\s", "");
        var last = s.substring(s.length() - 1);
        return "P-" + (base + last.repeat(4)).substring(0, 4).toUpperCase();
    }


    /// A product from the vocabulary: producer a character, name a thing, description a quote.
    public static Product random(RandomGenerator random, Instant at) {
        var person = Wonderland.CHARACTERS.next(random);
        var product = Wonderland.THINGS.next(random);
        var place = Wonderland.PLACES.next(random);
        var quote = Wonderland.QUOTES.next(random);
        return new Product(
                person.toUpperCase(),
                id(product),
                product,
                Wonderland.DESCRIPTIONS.next(random).formatted(product, place, person, quote),
                at);
    }
}
