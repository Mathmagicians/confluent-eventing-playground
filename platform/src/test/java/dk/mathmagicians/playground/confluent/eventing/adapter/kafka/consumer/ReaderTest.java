package dk.mathmagicians.playground.confluent.eventing.adapter.kafka.consumer;

import static dk.mathmagicians.playground.confluent.eventing.adapter.kafka.KafkaFixtures.topics;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.envelope;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.protobuf.Message;
import dk.mathmagicians.playground.confluent.eventing.adapter.kafka.EnvelopeHeaders;
import dk.mathmagicians.playground.confluent.eventing.adapter.protobuf.Converter;
import dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures;
import dk.mathmagicians.playground.confluent.eventing.domain.Payload;
import io.confluent.kafka.serializers.protobuf.KafkaProtobufSerializer;
import java.util.List;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;

/// The bytes off the wire: a message serialized as the publisher's serializer does it, magic byte and schema id
/// first, read back as its envelope. The registry is Confluent's in-memory one, `mock://`, shared by its name.
class ReaderTest {

    private static final String REGISTRY = "mock://reader";

    static List<Payload> payloads() {
        return EventFixtures.payloads();
    }

    @ParameterizedTest
    @MethodSource("payloads")
    void readsTheEnvelopeOffTheWire(Payload payload) {
        var envelope = envelope(payload);
        var topic = topics().select(payload);
        var message = Converter.to(payload);
        var record = new ConsumerRecord<>(topic, 0, 0L, envelope.key(), serialize(topic, message));
        EnvelopeHeaders.headers(envelope, message).forEach(record.headers()::add);
        var reader = new Reader(properties());

        assertThat(reader.read(record)).isEqualTo(envelope);
    }

    private static KafkaProperties properties() {
        var properties = new KafkaProperties();
        properties.getProperties().put("schema.registry.url", REGISTRY);
        return properties;
    }

    private static byte[] serialize(String topic, Message message) {
        try (var serializer = new KafkaProtobufSerializer<Message>()) {
            serializer.configure(Map.of("schema.registry.url", REGISTRY), false);
            return serializer.serialize(topic, message);
        }
    }
}
