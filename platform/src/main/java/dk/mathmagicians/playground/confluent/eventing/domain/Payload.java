package dk.mathmagicians.playground.confluent.eventing.domain;

import org.jmolecules.ddd.annotation.ValueObject;

import java.util.UUID;
import java.util.random.RandomGenerator;

/// The closed set of payload types. A switch over it is exhaustive.'
@ValueObject
public sealed interface Payload permits Product, Offer, Order, Transaction {

    /// The payload's own id, `P-POCK`, `OFF-<uuid>`, `ORD-<uuid>`, `TX-<order>-<offer>`: one name for it on every
    /// record, so a log line or a story handles any payload the same way.
    String id();

    /// An event id: should be  UUID type
    static String id(String prefix, RandomGenerator random) {
        var bytesArray = new byte[16];
        // Generate random bytes
        random.nextBytes( bytesArray );
        var uuid = UUID.nameUUIDFromBytes(bytesArray);
        return String.join("-", prefix, uuid.toString());
    }
}
