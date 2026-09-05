package dk.mathmagicians.playground.confluent.eventing.domain;

import java.time.Instant;
import java.util.random.RandomGenerator;

/// An event is an envelope with a payload. The header names the event, where it came from, and when.
public record Envelope(String id, String region, String app, Instant at, Payload payload) {

    /// An envelope id: `E` and sixteen hex digits, see `Payload.id`.
    public static String id(RandomGenerator random) {
        throw new UnsupportedOperationException("Envelope.id");
    }

    /// The partition key, one rule per payload type as the README Purpose shows: a product's id, an offer's region
    /// and product id joined by `/`, an order's region.
    public String key() {
        throw new UnsupportedOperationException("Envelope.key");
    }
}
