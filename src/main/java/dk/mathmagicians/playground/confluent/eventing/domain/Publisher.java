package dk.mathmagicians.playground.confluent.eventing.domain;

import java.util.concurrent.CompletableFuture;

/// Outbound port: where envelopes go. One adapter per profile, the log or Kafka. The receipt completes when the
/// message has landed.
@FunctionalInterface
public interface Publisher {

    CompletableFuture<Receipt> publish(Envelope envelope);
}
