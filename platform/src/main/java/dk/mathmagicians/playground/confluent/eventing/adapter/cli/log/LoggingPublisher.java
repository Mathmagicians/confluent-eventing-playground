package dk.mathmagicians.playground.confluent.eventing.adapter.cli.log;

import dk.mathmagicians.playground.confluent.eventing.adapter.protobuf.Converter;
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

/// Driven adapter for the `cli` profile: the log is the sink, every envelope at INFO, the payload as a line the
/// console reads. The receipt names the log as the topic and counts the lines as offsets.
@SecondaryAdapter
@Component
@Profile("cli")
public final class LoggingPublisher implements Publisher {

    static final String TOPIC = "log";

    /// the receipt in `KafkaPublisher`'s words, then the key, the region, and the payload as `Converter.text`
    public static final String PUBLISHED = "Published {} to {}-{} at offset {}, key {}, region {}: {}";

    private static final Logger log = LoggerFactory.getLogger(LoggingPublisher.class);

    private final AtomicLong offset = new AtomicLong();

    @Override
    public CompletableFuture<Receipt> publish(Envelope envelope) {
        var receipt = new Receipt(envelope.id(), TOPIC, 0, offset.getAndIncrement());
        log.info(PUBLISHED, receipt.envelopeId(), receipt.topic(), receipt.partition(), receipt.offset(),
                envelope.key(), envelope.region(), Converter.text(envelope.payload()));
        return CompletableFuture.completedFuture(receipt);
    }
}
