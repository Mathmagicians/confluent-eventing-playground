package dk.mathmagicians.playground.confluent.eventing.domain;

import java.time.Instant;
import java.util.random.RandomGenerator;

/// An event is an envelope with a payload. The header names the event, where it came from, and when.
public record Envelope(String id, String region, String app, Instant at, Payload payload) {
    static final String ID_PREFIX = "ENVL";

    public static String id(RandomGenerator random) {
        return Payload.id(ID_PREFIX, random);
    }

    interface KeyStrategy<T extends Payload> {
        String key(String region, T payload);
    }
    static final KeyStrategy<Product> PRODUCT_KEY_STRATEGY = (_, p) -> p.productId();
    static final KeyStrategy<Offer> OFFER_KEY_STRATEGY = (region, o) -> String.join("/", region, o.productId());
    static final KeyStrategy<Order> ORDER_KEY_STRATEGY = (region, o) -> region;
    static final KeyStrategy<Transaction> TRANSACTION_KEY_STRATEGY = (region, t) ->  t.customerId();

    public String key() {
        return switch (payload) {
            case Product product -> PRODUCT_KEY_STRATEGY.key(region, product);
            case Offer offer -> OFFER_KEY_STRATEGY.key(region, offer);
            case Order order -> ORDER_KEY_STRATEGY.key(region, order);
            case Transaction transaction -> TRANSACTION_KEY_STRATEGY.key(region, transaction);
        };
    }

    public static Envelope of(RandomGenerator random, String region, String app, Instant at, Payload payload) {
        return new Envelope(id(random), region, app, at, payload);
    }

}
