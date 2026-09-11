package dk.mathmagicians.playground.confluent.eventing.adapter.kafka.publisher;

import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.envelope;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.offer;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures;
import dk.mathmagicians.playground.confluent.eventing.domain.Payload;
import dk.mathmagicians.playground.eventing.OfferDTO;
import java.util.List;
import org.apache.kafka.common.header.Header;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/// The outbound direction alone; the round trips with the inbound one are in the consumer's `ReaderTest`.
class ConverterTest {

    static List<Payload> payloads() {
        return EventFixtures.payloads();
    }

    @ParameterizedTest
    @MethodSource("payloads")
    void makesTheMessageOfThePayloadsType(Payload payload) {
        var message = Converter.to(payload);

        assertThat(message.getDescriptorForType().getName()).isEqualTo(payload.getClass().getSimpleName());
    }

    @Test
    void copiesEveryFieldOfAnOffer() {
        var offer = offer();

        var message = Converter.to(offer);

        assertThat(message)
                .isEqualTo(OfferDTO.Offer.newBuilder()
                        .setOfferId(offer.offerId())
                        .setProductId(offer.productId())
                        .setPrice(offer.price())
                        .setSellerId(offer.sellerId())
                        .setCreatedAt(com.google.protobuf.Timestamp.newBuilder()
                                .setSeconds(offer.createdAt().getEpochSecond())
                                .setNanos(offer.createdAt().getNano()))
                        .build());
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
                        tuple("ce_id", envelope.id()),
                        tuple("ce_region", envelope.region()),
                        tuple("ce_source", envelope.app()),
                        tuple("ce_time", envelope.at().toString()),
                        tuple("ce_specversion", "1.0"),
                        tuple("ce_type", "dk.mathmagicians.playground.eventing.Offer"));
    }
}
