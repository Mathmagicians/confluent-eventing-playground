package dk.mathmagicians.playground.confluent.eventing.domain;

import java.time.Instant;

/// An event is an envelope with a payload. The header names the event, where it came from, and when.
public record Envelope(String id, String region, String app, Instant at, Payload payload) {
}
