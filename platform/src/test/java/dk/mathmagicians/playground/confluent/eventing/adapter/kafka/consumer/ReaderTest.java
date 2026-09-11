package dk.mathmagicians.playground.confluent.eventing.adapter.kafka.consumer;

import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.envelope;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.offer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import com.google.protobuf.DynamicMessage;
import com.google.protobuf.Timestamp;
import dk.mathmagicians.playground.confluent.eventing.adapter.kafka.publisher.Converter;
import dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures;
import dk.mathmagicians.playground.confluent.eventing.domain.Payload;
import java.util.List;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/// The inbound direction, and the round trips with the publisher's `Converter`. The deserializer itself needs
/// the registry, so `read` is exercised by the BDD suite; what comes after it is exercised here.
class ReaderTest {

    static List<Payload> payloads() {
        return EventFixtures.payloads();
    }

    @ParameterizedTest
    @MethodSource("payloads")
    void roundTripsThroughTheMessage(Payload payload) {
        var message = Converter.to(payload);

        assertThat(Reader.from(message)).isEqualTo(payload);
    }

    @ParameterizedTest
    @MethodSource("payloads")
    void roundTripsThroughBytes(Payload payload) throws Exception {
        var message = Converter.to(payload);
        var parsed = message.getParserForType().parseFrom(message.toByteArray());

        assertThat(Reader.from(parsed)).isEqualTo(payload);
    }

    /// The deserializer answers a `DynamicMessage` when it knows no generated class: the full name picks the
    /// record all the same.
    @ParameterizedTest
    @MethodSource("payloads")
    void readsADynamicMessageByItsFullName(Payload payload) throws Exception {
        var message = Converter.to(payload);
        var dynamic = DynamicMessage.parseFrom(message.getDescriptorForType(), message.toByteArray());

        assertThat(Reader.from(dynamic)).isEqualTo(payload);
    }

    @Test
    void rejectsAMessageOfNoPayloadType() {
        var timestamp = Timestamp.newBuilder().setSeconds(1).build();

        assertThatIllegalArgumentException()
                .isThrownBy(() -> Reader.from(timestamp))
                .withMessageContaining("google.protobuf.Timestamp");
    }

    /// Ties the fixture list to the permitted records, so a new record without a fixture fails here.
    @Test
    void everyPermittedRecordHasAFixture() {
        Assertions.<Class<?>>assertThat(EventFixtures.payloads().stream().map(Payload::getClass))
                .containsExactlyInAnyOrder(Payload.class.getPermittedSubclasses());
    }

    @ParameterizedTest
    @MethodSource("payloads")
    void envelopeRoundTripsThroughTheHeaders(Payload payload) {
        var envelope = envelope(payload);
        var headers = new RecordHeaders(Converter.headers(envelope));

        assertThat(Reader.envelope(headers, payload)).isEqualTo(envelope);
    }

    @Test
    void rejectsARecordWithoutTheEnvelopeHeaders() {
        var none = new RecordHeaders();

        assertThatIllegalArgumentException()
                .isThrownBy(() -> Reader.envelope(none, offer()))
                .withMessageContaining("ce_id");
    }
}
