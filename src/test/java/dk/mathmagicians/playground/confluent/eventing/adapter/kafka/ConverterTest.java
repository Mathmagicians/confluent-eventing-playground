package dk.mathmagicians.playground.confluent.eventing.adapter.kafka;

import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.envelope;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.offer;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures;
import dk.mathmagicians.playground.confluent.eventing.domain.Payload;
import java.util.List;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeaders;
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
    void envelopeRoundTripsThroughTheHeaders(Payload payload) {
        var envelope = envelope(payload);

        var headers = new RecordHeaders(Converter.headers(envelope));

        assertThat(Converter.envelope(headers, payload)).isEqualTo(envelope);
    }

    @Test
    void headersCarryTheCloudEventsNames() {
        var headers = Converter.headers(envelope());

        assertThat(headers)
                .extracting(Header::key)
                .containsExactly("ce_id", "ce_region", "ce_source", "ce_time", "ce_specversion", "ce_type");
    }

    @Test
    void headersNameTheSpecVersionAndThePayloadsMessage() {
        var envelope = envelope(offer());

        var headers = Converter.headers(envelope);

        assertThat(headers)
                .extracting(Header::key, header -> new String(header.value(), UTF_8))
                .contains(
                        tuple("ce_specversion", "1.0"),
                        tuple("ce_type", "dk.mathmagicians.playground.eventing.Offer"));
    }

    @Test
    void rejectsARecordWithoutTheEnvelopeHeaders() {
        var none = new RecordHeaders();

        assertThatThrownBy(() -> Converter.envelope(none, offer()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ce_id");
    }
}
