package dk.mathmagicians.playground.confluent.eventing.dto;

import java.time.Instant;

/// The thin envelope on the wire: header fields plus one payload, carried as `google.protobuf.Any`.
public record Envelope<T extends Record>(String id, String region, String appid, Instant timestamp, T payload) {
}
