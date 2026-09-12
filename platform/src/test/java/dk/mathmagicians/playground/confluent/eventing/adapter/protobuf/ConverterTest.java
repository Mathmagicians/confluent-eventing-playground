package dk.mathmagicians.playground.confluent.eventing.adapter.protobuf;

import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.offer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import com.google.protobuf.DynamicMessage;
import com.google.protobuf.TextFormat;
import com.google.protobuf.Timestamp;
import dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures;
import dk.mathmagicians.playground.confluent.eventing.domain.Payload;
import dk.mathmagicians.playground.eventing.OfferDTO;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/// Both directions, the round trips between them, and the builders the console fills from text.
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
                        .setOfferId(offer.id())
                        .setProductId(offer.productId())
                        .setPrice(offer.price())
                        .setSellerId(offer.sellerId())
                        .setCreatedAt(Timestamp.newBuilder()
                                .setSeconds(offer.createdAt().getEpochSecond())
                                .setNanos(offer.createdAt().getNano()))
                        .build());
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

    /// The deserializer answers a `DynamicMessage` when it knows no generated class: the full name picks the
    /// record all the same.
    @ParameterizedTest
    @MethodSource("payloads")
    void readsADynamicMessageByItsFullName(Payload payload) throws Exception {
        var message = Converter.to(payload);
        var dynamic = DynamicMessage.parseFrom(message.getDescriptorForType(), message.toByteArray());

        assertThat(Converter.from(dynamic)).isEqualTo(payload);
    }

    @Test
    void rejectsAMessageOfNoPayloadType() {
        var timestamp = Timestamp.newBuilder().setSeconds(1).build();

        assertThatIllegalArgumentException()
                .isThrownBy(() -> Converter.from(timestamp))
                .withMessageContaining("google.protobuf.Timestamp");
    }

    /// Ties the fixture list to the permitted records, so a new record without a fixture fails here.
    @Test
    void everyPermittedRecordHasAFixture() {
        Assertions.<Class<?>>assertThat(EventFixtures.payloads().stream().map(Payload::getClass))
                .containsExactlyInAnyOrder(Payload.class.getPermittedSubclasses());
    }

    @Test
    void namesEveryPayloadType() {
        assertThat(Converter.types()).containsExactly("Offer", "Order", "Product", "Transaction");
    }

    /// The console's way in: a builder by the type's name, filled from Protobuf text, the record from it.
    @Test
    void buildsAMessageOfATypeFromText() throws Exception {
        var builder = Converter.builder("Offer");
        TextFormat.merge("offer_id: \"OFF-1\" product_id: \"P-TOPH\" price: 12.5 seller_id: \"MAD_HATTER\"", builder);

        var payload = Converter.from(builder.build());

        assertThat(payload).isEqualTo(Converter.from(OfferDTO.Offer.newBuilder()
                .setOfferId("OFF-1").setProductId("P-TOPH").setPrice(12.5).setSellerId("MAD_HATTER").build()));
    }

    @Test
    void refusesABuilderOfNoPayloadTypeNamingTheTypes() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Converter.builder("Teapot"))
                .withMessageContaining("Teapot is no payload type")
                .withMessageContaining("Offer");
    }
}
