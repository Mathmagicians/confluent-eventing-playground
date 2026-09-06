package dk.mathmagicians.playground.confluent.eventing.dto;

import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.envelope;
import static org.assertj.core.api.Assertions.*;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Timestamp;
import dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures;
import dk.mathmagicians.playground.confluent.eventing.domain.Payload;
import dk.mathmagicians.playground.eventing.Schemas;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ConverterTest {

    static List<Payload> payloads() {
        return EventFixtures.payloads();
    }

    @ParameterizedTest
    @MethodSource("payloads")
    void roundTripsThroughTheMessage(Payload payload) {
        var message = Converter.to(payload);
        assertThat(Converter.from(message)).isEqualTo(payload);
    }

    @ParameterizedTest
    @MethodSource("payloads")
    void roundTripsThroughBytes(Payload payload) throws Exception {
        var message = Converter.to(payload);
        var parsed = message.getParserForType().parseFrom(message.toByteArray());
        assertThat(Converter.from(parsed)).isEqualTo(payload);
    }

    /// Ties the fixture list to the permitted records, so a new record without a fixture fails here.
    @Test
    void everyPermittedRecordHasAFixture() {
        Assertions.<Class<?>>assertThat(EventFixtures.payloads().stream().map(Payload::getClass))
                .containsExactlyInAnyOrder(Payload.class.getPermittedSubclasses());
    }

    @ParameterizedTest
    @MethodSource("payloads")
    void envelopeRoundTripsThroughTheMessage(Payload payload) {
        var message = Converter.to(envelope(payload));

        assertThat(Converter.from(message)).isEqualTo(envelope(payload));
    }

    @ParameterizedTest
    @MethodSource("payloads")
    void envelopeRoundTripsThroughBytes(Payload payload) throws InvalidProtocolBufferException {
        var message = Converter.to(envelope(payload));

        var parsed = Schemas.Envelope.parseFrom(message.toByteArray());

        assertThat(Converter.from(parsed)).isEqualTo(envelope(payload));
    }

    /// An envelope whose `Any` names a type outside `PAYLOADS`.
    @Test
    void envelopeRejectsAnUnknownPayload() {
        var stranger = Schemas.Envelope.newBuilder().setPayload(Any.pack(Timestamp.getDefaultInstance())).build();

        assertThatThrownBy(() -> Converter.from(stranger))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Timestamp");
    }
}
