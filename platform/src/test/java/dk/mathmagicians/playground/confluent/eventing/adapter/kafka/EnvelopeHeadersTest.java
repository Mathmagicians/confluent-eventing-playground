package dk.mathmagicians.playground.confluent.eventing.adapter.kafka;

import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.envelope;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.offer;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.tuple;

import dk.mathmagicians.playground.confluent.eventing.adapter.protobuf.Converter;
import dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures;
import dk.mathmagicians.playground.confluent.eventing.domain.Payload;
import java.util.List;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/// The CloudEvents headers both ways: written by the publisher, read by the consumer.
class EnvelopeHeadersTest {

    static List<Payload> payloads() {
        return EventFixtures.payloads();
    }

    @Test
    void headersCarryTheCloudEventsNames() {
        var envelope = envelope();

        var headers = EnvelopeHeaders.headers(envelope, Converter.to(envelope.payload()));

        assertThat(headers)
                .extracting(Header::key)
                .containsExactly("ce_id", "ce_region", "ce_source", "ce_time", "ce_specversion", "ce_type");
    }

    @Test
    void headersNameTheSpecVersionAndThePayloadsMessage() {
        var envelope = envelope(offer());

        var headers = EnvelopeHeaders.headers(envelope, Converter.to(envelope.payload()));

        assertThat(headers)
                .extracting(Header::key, header -> new String(header.value(), UTF_8))
                .contains(
                        tuple("ce_id", envelope.id()),
                        tuple("ce_region", envelope.region()),
                        tuple("ce_source", envelope.app()),
                        tuple("ce_time", envelope.at().toString()),
                        tuple("ce_specversion", "1.0"),
                        tuple("ce_type", "dk.mathmagicians.playground.eventing.Offer"));
    }

    @ParameterizedTest
    @MethodSource("payloads")
    void envelopeRoundTripsThroughTheHeaders(Payload payload) {
        var envelope = envelope(payload);
        var headers = new RecordHeaders(EnvelopeHeaders.headers(envelope, Converter.to(payload)));

        assertThat(EnvelopeHeaders.envelope(headers, payload)).isEqualTo(envelope);
    }

    @Test
    void rejectsARecordWithoutTheEnvelopeHeaders() {
        var none = new RecordHeaders();

        assertThatIllegalArgumentException()
                .isThrownBy(() -> EnvelopeHeaders.envelope(none, offer()))
                .withMessageContaining("ce_id");
    }
}
