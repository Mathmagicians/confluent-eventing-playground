package dk.mathmagicians.playground.confluent.eventing.adapter.protobuf;

import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.offer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import com.google.protobuf.DynamicMessage;
import com.google.protobuf.Timestamp;
import dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures;
import dk.mathmagicians.playground.confluent.eventing.domain.Offer;
import dk.mathmagicians.playground.confluent.eventing.domain.Payload;
import dk.mathmagicians.playground.confluent.eventing.domain.Transaction;
import dk.mathmagicians.playground.eventing.OfferDTO;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/// Both directions, the round trips between them, and the text the console types: a record read from it, the
/// shape of a type as such text.
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

    /// The console's way in: the type's message filled from Protobuf text, the record of that type from it.
    @Test
    void readsARecordOfATypeFromText() {
        var offer = Converter.from(Offer.class,
                "offer_id: \"OFF-1\" product_id: \"P-TOPH\" price: 12.5 seller_id: \"MAD_HATTER\"");

        assertThat(offer).isEqualTo(Converter.from(OfferDTO.Offer.newBuilder()
                .setOfferId("OFF-1").setProductId("P-TOPH").setPrice(12.5).setSellerId("MAD_HATTER").build()));
    }

    @Test
    void refusesTextTheSchemaRejectsWithTheReason() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Converter.from(Offer.class, "prize: 12.5"))
                .withMessageContaining("no Offer in 'prize: 12.5'")
                .withMessageContaining("prize");
    }

    /// The shape: every field of the schema with an empty value, a nested message unfolded.
    @Test
    void shapesATypeAsALineOfEmptyFields() {
        assertThat(Converter.shape(Offer.class))
                .isEqualTo("Offer offer_id: \"\" product_id: \"\" price: 0 seller_id: \"\" created_at { seconds: 0 nanos: 0 }");
        assertThat(Converter.shape(Transaction.class))
                .startsWith("Transaction transaction_id: \"\" order_ref { id: \"\" customer_id: \"\"");
    }

    /// A shape is a line the console reads back: its fields, after the type's name, make a record of the type.
    @ParameterizedTest
    @MethodSource("payloads")
    void aShapeReadsBackAsARecordOfItsType(Payload payload) {
        var type = payload.getClass();
        var shape = Converter.shape(type);

        assertThat(Converter.from(type, shape.substring(type.getSimpleName().length()))).isInstanceOf(type);
    }
}
