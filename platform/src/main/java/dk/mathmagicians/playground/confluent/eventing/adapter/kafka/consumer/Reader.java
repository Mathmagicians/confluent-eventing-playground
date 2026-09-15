package dk.mathmagicians.playground.confluent.eventing.adapter.kafka.consumer;

import com.google.protobuf.Message;
import dk.mathmagicians.playground.confluent.eventing.adapter.kafka.EnvelopeHeaders;
import dk.mathmagicians.playground.confluent.eventing.adapter.protobuf.Converter;
import dk.mathmagicians.playground.confluent.eventing.domain.Envelope;
import io.confluent.kafka.serializers.protobuf.KafkaProtobufDeserializer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/// The wire format inbound. A record's value is the bytes on the wire: Confluent's magic byte, the schema id, and
/// the message. The reader owns the Protobuf deserializer, configured like the consumer, which resolves the id
/// through the registry and answers a message; `Converter` makes the record, `EnvelopeHeaders` the envelope. The
/// publisher is the other direction.
@Component
@Profile("!local")
public final class Reader {

    private final KafkaProtobufDeserializer<Message> deserializer;

    Reader(KafkaProperties properties) {
        this.deserializer = new KafkaProtobufDeserializer<>();
        this.deserializer.configure(properties.buildConsumerProperties(), false);
    }

    /// The envelope a record carries: the payload from its value, the header fields from its headers.
    public Envelope read(ConsumerRecord<String, byte[]> record) {
        var message = deserializer.deserialize(record.topic(), record.headers(), record.value());
        return EnvelopeHeaders.envelope(record.headers(), Converter.from(message));
    }
}
