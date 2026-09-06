package dk.mathmagicians.playground.confluent.eventing.application;

import dk.mathmagicians.playground.confluent.eventing.domain.Envelope;
import dk.mathmagicians.playground.confluent.eventing.domain.Receipt;
import java.util.concurrent.CompletableFuture;
import org.jmolecules.architecture.hexagonal.PrimaryPort;

/// Driving port, the feature's use case: a generator publishes a payload in an envelope and receives where the
/// message landed.
@PrimaryPort
@FunctionalInterface
public interface PublishMessage {

    CompletableFuture<Receipt> publish(Envelope envelope);
}
