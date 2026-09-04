package dk.mathmagicians.playground.confluent.eventing.domain;

import java.util.random.RandomGenerator;

/// The closed set of payload types. A switch over it is exhaustive.
public sealed interface Payload permits Product, Offer, Order, Transaction {

    /// An event id: the prefix and sixteen hex digits of a random long.
    static String id(String prefix, RandomGenerator random) {
        return "%s-%016x".formatted(prefix, random.nextLong());
    }
}
