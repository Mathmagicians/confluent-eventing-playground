package dk.mathmagicians.playground.confluent.eventing.domain;

import static dk.mathmagicians.playground.confluent.eventing.domain.Envelope.*;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

class EnvelopeTest {

    static Stream<Envelope> envelopes() {
        return Stream.of( envelope(product()), envelope(offer()), envelope(order()));
    }

    @Test
    void idIsAreUnique() {
        var ids = sample((random, _) -> Envelope.id(random)).toList();
        assertThat(ids).doesNotHaveDuplicates();
    }

    @Test
    void keyOfAProductIsItsId() {
        var product = product();
        var key = envelope(product).key();
        assertThat(key).isEqualTo( PRODUCT_KEY_STRATEGY.key(REGION, product)).isEqualTo(product.productId());
    }

    @Test
    void keyOfAnOfferIsTheRegionAndTheProductId() {
        var offer = offer();
        var key = envelope(offer).key();
        assertThat(key).isEqualTo(OFFER_KEY_STRATEGY.key(REGION, offer)).isEqualTo(REGION + "/" + offer.productId());
    }

    @Test
    void keyOfAnOrderIsTheRegion() {
        var key = envelope(order()).key();
        assertThat(key).isEqualTo(ORDER_KEY_STRATEGY.key(REGION, order())).isEqualTo(REGION);
    }

    @Test
    void keyOfATransactionIsTheCustomerId() {
        var transaction = EventFixtures.transaction();
        var key = envelope(transaction).key();
        assertThat(key).isEqualTo(TRANSACTION_KEY_STRATEGY.key(REGION, transaction)).isEqualTo(transaction.customerId());
    }
}
