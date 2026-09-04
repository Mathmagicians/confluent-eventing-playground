package dk.mathmagicians.playground.confluent.eventing.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.protobuf.Any;
import dk.mathmagicians.playground.eventing.Schemas;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

/// What one Offer looks like on the wire. Every field is a tag byte, `(field number << 3) | wire type`,
/// followed by the value. Wire type 2 is length-delimited (strings, nested messages), wire type 1 is 64-bit.
class OfferWireFormatTest {

    private static final Schemas.Offer OFFER = Schemas.Offer.newBuilder()
            .setOfferId("o1")
            .setProductId("p1")
            .setPrice(1.5)
            .setSellerId("s1")
            .build();

    // field 1, wire type 2: tag 0x0a, length 2, "o1"
    private static final String OFFER_ID = "0a02" + "6f31";
    // field 2, wire type 2: tag 0x12, length 2, "p1"
    private static final String PRODUCT_ID = "1202" + "7031";
    // field 3, wire type 1: tag 0x19, IEEE 754 double 1.5 = 0x3FF8000000000000, little-endian
    private static final String PRICE = "19" + "000000000000f83f";
    // field 4, wire type 2: tag 0x22, length 2, "s1"
    private static final String SELLER_ID = "2202" + "7331";

    @Test
    void encodesFieldsInNumberOrderAsTagThenValue() {
        var hex = HexFormat.of().formatHex(OFFER.toByteArray());

        assertThat(hex).isEqualTo(OFFER_ID + PRODUCT_ID + PRICE + SELLER_ID);
    }

    @Test
    void unsetMessageFieldTakesNoBytes() {
        var withTimestamp = OFFER.toBuilder()
                .setCreatedAt(com.google.protobuf.Timestamp.newBuilder().setSeconds(1).build())
                .build();

        // field 5, wire type 2: tag 0x2a, length 2, then Timestamp{seconds=1} as field 1 varint 1
        assertThat(HexFormat.of().formatHex(withTimestamp.toByteArray()))
                .isEqualTo(OFFER_ID + PRODUCT_ID + PRICE + SELLER_ID + "2a02" + "0801");
        assertThat(OFFER.getSerializedSize()).isEqualTo(21);
    }

    @Test
    void parsesBackToAnEqualMessage() throws Exception {
        var parsed = Schemas.Offer.parseFrom(OFFER.toByteArray());

        assertThat(parsed).isEqualTo(OFFER);
    }

    @Test
    void packedInAnEnvelopeCostsTheTypeUrl() {
        var envelope = Schemas.Envelope.newBuilder().setPayload(Any.pack(OFFER)).build();
        var typeUrl = "type.googleapis.com/dk.mathmagicians.playground.eventing.Offer";
        var typeUrlHex = HexFormat.of().formatHex(typeUrl.getBytes(StandardCharsets.UTF_8));

        var hex = HexFormat.of().formatHex(envelope.toByteArray());

        // field 5 (payload), wire type 2: tag 0x2a, length 87, then Any:
        //   field 1 type_url: tag 0x0a, length 62, the URL
        //   field 2 value:    tag 0x12, length 21, the Offer bytes from above
        assertThat(hex).isEqualTo("2a57" + "0a3e" + typeUrlHex + "1215" + OFFER_ID + PRODUCT_ID + PRICE + SELLER_ID);
        assertThat(envelope.getSerializedSize() - OFFER.getSerializedSize()).isEqualTo(68);
    }
}
