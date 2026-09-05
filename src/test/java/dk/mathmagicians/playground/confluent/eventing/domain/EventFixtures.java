package dk.mathmagicians.playground.confluent.eventing.domain;

import java.time.Instant;
import java.util.List;
import java.util.function.BiFunction;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;
import java.util.stream.Stream;

public final class EventFixtures {

    public static final Instant AT = Instant.parse("2026-09-04T10:15:30.123456789Z");

    public static final String REGION = "EMEA";

    public static final String APP = "load-generator";

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
        return new Envelope("E-0000000000000001", REGION, APP, AT, payload);
    }

    private EventFixtures() {
    }
}
