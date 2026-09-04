package dk.mathmagicians.playground.confluent.eventing.dto;

import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.envelope;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.offer;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.order;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.product;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.transaction;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dk.mathmagicians.playground.confluent.eventing.domain.Offer;
import dk.mathmagicians.playground.confluent.eventing.domain.Order;
import dk.mathmagicians.playground.confluent.eventing.domain.Product;
import dk.mathmagicians.playground.confluent.eventing.domain.Transaction;
import dk.mathmagicians.playground.eventing.Schemas;
import java.util.List;
import org.junit.jupiter.api.Test;

class ConverterTest {

    private static final Converter<Offer, Schemas.Offer> OFFERS =
            new Converter<>(Offer.class, Schemas.Offer.getDefaultInstance());

    @Test
    void envelopeRoundTripsPackedOffer() {
        var converter = new Converter<>(Envelope.class, Schemas.Envelope.getDefaultInstance(), List.of(OFFERS));

        var back = converter.from(converter.to(envelope()));

        assertThat(back).isEqualTo(envelope());
    }

    @Test
    void envelopeRejectsUnregisteredPayload() {
        var converter = new Converter<>(Envelope.class, Schemas.Envelope.getDefaultInstance());

        assertThatThrownBy(() -> converter.to(envelope()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Offer");
    }

    @Test
    void productRoundTrips() {
        var converter = new Converter<>(Product.class, Schemas.Product.getDefaultInstance());

        var back = converter.from(converter.to(product()));

        assertThat(back).isEqualTo(product());
    }

    @Test
    void orderRoundTripsRepeatedProducts() {
        var converter = new Converter<>(Order.class, Schemas.Order.getDefaultInstance());

        var back = converter.from(converter.to(order()));

        assertThat(back).isEqualTo(order());
    }

    @Test
    void offerRoundTripsDouble() {
        var converter = new Converter<>(Offer.class, Schemas.Offer.getDefaultInstance());

        var back = converter.from(converter.to(offer()));

        assertThat(back).isEqualTo(offer());
    }

    @Test
    void transactionRoundTripsNestedRecords() {
        var converter = new Converter<>(Transaction.class, Schemas.Transaction.getDefaultInstance());

        var back = converter.from(converter.to(transaction()));

        assertThat(back).isEqualTo(transaction());
    }

    @Test
    void timestampKeepsNanos() {
        var proto = OFFERS.to(offer());

        assertThat(proto.getCreatedAt().getSeconds()).isEqualTo(offer().createdAt().getEpochSecond());
        assertThat(proto.getCreatedAt().getNanos()).isEqualTo(offer().createdAt().getNano());
    }

    @Test
    void rejectsRecordComponentWithoutProtoField() {
        record Stranger(String nobody) {
        }

        assertThatThrownBy(() -> new Converter<>(Stranger.class, Schemas.Offer.getDefaultInstance()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nobody");
    }

    @Test
    void rejectsProtoFieldWithoutRecordComponent() {
        record Partial(String offerId) {
        }

        assertThatThrownBy(() -> new Converter<>(Partial.class, Schemas.Offer.getDefaultInstance()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("product_id");
    }
}
