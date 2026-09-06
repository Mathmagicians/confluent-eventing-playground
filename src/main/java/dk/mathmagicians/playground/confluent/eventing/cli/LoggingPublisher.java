package dk.mathmagicians.playground.confluent.eventing.cli;

import dk.mathmagicians.playground.confluent.eventing.application.Publisher;
import dk.mathmagicians.playground.confluent.eventing.domain.Envelope;
import dk.mathmagicians.playground.confluent.eventing.domain.Receipt;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/// Driven adapter for the `local` profile: the log is the sink, every envelope at INFO. The receipt names the log
/// as the topic and counts the lines as offsets.
@SecondaryAdapter
@Component
@Profile("local")
final class LoggingPublisher implements Publisher {

    static final String TOPIC = "log";

    private static final Logger log = LoggerFactory.getLogger(LoggingPublisher.class);

    private final AtomicLong offset = new AtomicLong();

    @Override
    public CompletableFuture<Receipt> publish(Envelope envelope) {
        log.info("Published {} key {}: {}", envelope.id(), envelope.key(), envelope.payload());
        return CompletableFuture.completedFuture(new Receipt(envelope.id(), TOPIC, 0, offset.getAndIncrement()));
    }
}
