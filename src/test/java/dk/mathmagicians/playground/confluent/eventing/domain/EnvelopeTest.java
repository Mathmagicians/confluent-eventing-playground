package dk.mathmagicians.playground.confluent.eventing.domain;

import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.REGION;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.envelope;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.offer;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.order;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.product;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.sample;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EnvelopeTest {

    @Test
    void idHasThePrefixAndSixteenHexDigits() {
        var ids = sample((random, _) -> Envelope.id(random));

        assertThat(ids).allMatch(id -> id.matches("E-[0-9a-f]{16}"));
    }

    @Test
    void keyOfAProductIsItsId() {
        var product = product();

        var key = envelope(product).key();

        assertThat(key).isEqualTo(product.productId());
    }

    @Test
    void keyOfAnOfferIsTheRegionAndTheProductId() {
        var offer = offer();

        var key = envelope(offer).key();

        assertThat(key).isEqualTo(REGION + "/" + offer.productId());
    }

    @Test
    void keyOfAnOrderIsTheRegion() {
        var envelope = envelope(order());

        assertThat(envelope.key()).isEqualTo(REGION);
    }
}
