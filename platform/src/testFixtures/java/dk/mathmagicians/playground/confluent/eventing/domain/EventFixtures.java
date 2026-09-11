package dk.mathmagicians.playground.confluent.eventing.domain;

import dk.mathmagicians.playground.confluent.eventing.application.Publisher;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;
import java.util.stream.Stream;

public final class EventFixtures {

    public static final Instant AT = Instant.parse("2026-09-04T10:15:30.123456789Z");

    public static final String REGION = "EMEA";

    /// `spring.application.name`
    public static final String APP = "confluent-eventing-playground";

    /// Rule Forty-two, the oldest rule in the book.
    public static final long SEED = 42;

    /// Draws per sample.
    public static final int DRAWS = 7;

    /// A fresh generator seeded with `SEED`: the same sequence in every test.
    public static RandomGenerator dice() {
        return RandomGeneratorFactory.getDefault().create(SEED);
    }

    /// `DRAWS` from one seeded generator, so the draws differ and the sample is the same in every test.
    public static <T> Stream<T> sample(BiFunction<RandomGenerator, Instant, T> recipe) {
        var dice = dice();
        return Stream.generate(() -> recipe.apply(dice, AT)).limit(DRAWS);
    }

    public static Product product() {
        return Product.random(dice(), AT);
    }

    public static Order order() {
        return Order.random(dice(), AT);
    }

    public static Offer offer() {
        return Offer.random(dice(), AT);
    }

    public static Transaction transaction() {
        return Transaction.random(dice(), AT);
    }

    /// One fixture per record `Payload` permits.
    public static List<Payload> payloads() {

        return List.of(product(), offer(), order(), transaction());
    }

    public static Envelope envelope() {
        return envelope(offer());
    }

    public static Envelope envelope(Payload payload) {
        return Envelope.of( dice(),  REGION, APP, AT, payload);
    }

    /// A publisher that keeps every envelope it gets and answers at once with `receipt`.
    public static Publisher publisher(Collection<Envelope> published) {
        return envelope -> {
            published.add(envelope);
            return CompletableFuture.completedFuture(receipt(envelope));
        };
    }

    /// The first offset of the first partition of `orders`.
    public static Receipt receipt(Envelope envelope) {
        return new Receipt(envelope.id(), "orders", 0, 0);
    }

    /// The id of a character on the wire, the domain's rule: `Mad Hatter` as the domain writes it.
    public static String character(String name) {
        return Wonderland.characterId(name);
    }

    /// The id of a thing, the product key, the domain's rule: `Top Hat` as the domain writes it.
    public static String thing(String name) {
        return Product.id(name);
    }

    private EventFixtures() {
    }
}
