package dk.mathmagicians.playground.confluent.eventing.domain;

import java.util.UUID;
import java.util.random.RandomGenerator;

/// The closed set of payload types. A switch over it is exhaustive.
public sealed interface Payload permits Product, Offer, Order, Transaction {

    /// An event id: should be  UUID type
    static String id(String prefix, RandomGenerator random) {
        var bytesArray = new byte[16];
        // Generate random bytes
        random.nextBytes( bytesArray );
        var uuid = UUID.nameUUIDFromBytes(bytesArray);
        return String.join("-", prefix, uuid.toString());
    }
}
