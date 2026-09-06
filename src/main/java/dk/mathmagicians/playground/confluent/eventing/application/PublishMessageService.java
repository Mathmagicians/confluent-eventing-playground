package dk.mathmagicians.playground.confluent.eventing.application;

import dk.mathmagicians.playground.confluent.eventing.domain.Envelope;
import dk.mathmagicians.playground.confluent.eventing.domain.Receipt;
import java.util.concurrent.CompletableFuture;
import org.jmolecules.architecture.hexagonal.Application;

/// Hands the envelope to whichever publisher the profile wired and answers its receipt.
@Application
public record PublishMessageService(Publisher publisher) implements PublishMessage {

    @Override
    public CompletableFuture<Receipt> publish(Envelope envelope) {
        return publisher.publish(envelope);
    }
}
