package dk.mathmagicians.playground.confluent.eventing.adapter.kafka;

import com.google.protobuf.Message;
import dk.mathmagicians.playground.confluent.eventing.application.Publisher;
import dk.mathmagicians.playground.confluent.eventing.domain.Envelope;
import dk.mathmagicians.playground.confluent.eventing.domain.Receipt;
import java.util.concurrent.CompletableFuture;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/// Driven adapter for `test` and `prod`: the envelope as record headers, the payload as its Protobuf message, on
/// the topic of its payload type, keyed by `Envelope.key()`. The serializer configured in the properties writes
/// the message with the schema id from the registry. The receipt carries the partition and offset the broker
/// acknowledged; a failed send is logged at ERROR with the envelope id and fails the receipt.
@SecondaryAdapter
@Component
@Profile("!local")
final class KafkaPublisher implements Publisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaPublisher.class);

    private final KafkaTemplate<String, Message> template;
    private final Topics topics;

    KafkaPublisher(KafkaTemplate<String, Message> template, Topics topics) {
        this.template = template;
        this.topics = topics;
    }

    @Override
    public CompletableFuture<Receipt> publish(Envelope envelope) {
        var topic = topics.select(envelope.payload());
        var value = Converter.to(envelope.payload());
        var record = new ProducerRecord<>(topic, null, envelope.key(), value, Converter.headers(envelope));
        return template.send(record)
                .whenComplete((result, failure) -> {
                    if (failure != null) {
                        log.error("Publishing {} to {} failed", envelope.id(), topic, failure);
                    } else {
                        var metadata = result.getRecordMetadata();
                        log.debug("Published {} to {}-{} at offset {}",
                                envelope.id(), metadata.topic(), metadata.partition(), metadata.offset());
                    }
                })
                .thenApply(result -> receipt(envelope, result.getRecordMetadata()));
    }

    private static Receipt receipt(Envelope envelope, RecordMetadata metadata) {
        return new Receipt(envelope.id(), metadata.topic(), metadata.partition(), metadata.offset());
    }
}
