package dk.mathmagicians.playground.confluent.eventing.dto;

import static org.assertj.core.api.Assertions.*;

import dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures;
import java.util.List;

import dk.mathmagicians.playground.confluent.eventing.domain.Payload;
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

    /// Guard - Ties the fixture list to `Payload.class.getPermittedSubclasses()`, so a new record without a fixture fails here.
    @Test
    void everyPermittedRecordHasAFixture() {
        Assertions.<Class<?>>assertThat(EventFixtures.payloads().stream().map(Payload::getClass))
                .containsExactlyInAnyOrder(Payload.class.getPermittedSubclasses());
    }

    @ParameterizedTest
    @MethodSource("payloads")
    void envelopeRoundTripsThroughTheMessage(Payload payload) {
        fail("not implemented");
    }

    @ParameterizedTest
    @MethodSource("payloads")
    void envelopeRoundTripsThroughBytes(Payload payload) {
        fail("not implemented");
    }

    /// An envelope whose `Any` names a type outside `PAYLOADS`.
    @Test
    void envelopeRejectsAnUnknownPayload() {
        fail("not implemented");
    }
}
