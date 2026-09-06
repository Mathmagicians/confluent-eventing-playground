package dk.mathmagicians.playground.confluent.eventing.kafka;

import dk.mathmagicians.playground.confluent.eventing.domain.Envelope;
import dk.mathmagicians.playground.confluent.eventing.domain.Publisher;
import dk.mathmagicians.playground.confluent.eventing.dto.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/// Outbound adapter for `test` and `prod`: the envelope as Protobuf bytes, `Converter.to(envelope).toByteArray()`,
/// on the topic of its payload type, keyed by `Envelope.key()`. A failed send is logged at ERROR with the envelope
/// id.
@Component
@Profile("!local")
final class KafkaPublisher implements Publisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaPublisher.class);

    private final KafkaTemplate<String, byte[]> template;
    private final Topics topics;

    KafkaPublisher(KafkaTemplate<String, byte[]> template, Topics topics) {
        this.template = template;
        this.topics = topics;
    }

    @Override
    public void publish(Envelope envelope) {
        var bytes = Converter.to(envelope).toByteArray();
        var topic = topics.select(envelope.payload());
        template.send(topic, envelope.key(), bytes).whenComplete((result, failure) -> {
            if (failure != null) {
                log.error("Publishing {} to {} failed", envelope.id(), topic, failure);
            } else {
                var metadata = result.getRecordMetadata();
                log.debug("Published {} to {}-{} at offset {}",
                        envelope.id(), metadata.topic(), metadata.partition(), metadata.offset());
            }
        });
    }
}
