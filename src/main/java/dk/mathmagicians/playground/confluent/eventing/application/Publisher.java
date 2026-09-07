package dk.mathmagicians.playground.confluent.eventing.application;

import dk.mathmagicians.playground.confluent.eventing.domain.Envelope;
import dk.mathmagicians.playground.confluent.eventing.domain.Receipt;
import java.util.concurrent.CompletableFuture;
import org.jmolecules.architecture.hexagonal.SecondaryPort;

/// Driven port: where envelopes go. One adapter per profile, the log or Kafka. The receipt completes when the
/// message has landed.
@SecondaryPort
@FunctionalInterface
public interface Publisher {

    CompletableFuture<Receipt> publish(Envelope envelope);
}
