package dk.mathmagicians.playground.confluent.stories.teaparty;

import static dk.mathmagicians.playground.confluent.eventing.application.StoryFixtures.publishMessage;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.ALICE;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.CLOCK;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.DORMOUSE;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.MAD_HATTER;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.MARCH_HARE;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.REGION;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.TARTS;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.TOP_HAT;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.envelope;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.offer;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.order;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.product;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import dk.mathmagicians.playground.confluent.eventing.domain.Envelope;
import dk.mathmagicians.playground.confluent.eventing.domain.Offer;
import dk.mathmagicians.playground.confluent.eventing.domain.Order;
import dk.mathmagicians.playground.confluent.eventing.domain.Transaction;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/// The tea party over offers and orders built by hand: an offer is one thing for sale, an order one thing wanted,
/// and what it publishes is what it settled. Every test gets a party of its own, so nothing waits over from one
/// test to the next.
class SettleAtTheTeaPartyTest {

    private static final Duration TTL = Duration.ofSeconds(30);

    /// Envelopes as the tea party publishes them.
    private final List<Envelope> published = new ArrayList<>();
    private final SettleAtTheTeaParty teaParty =
            new SettleAtTheTeaParty(REGION, publishMessage(published), CLOCK, TTL);

    private List<String> settledOrders() {
        return published.stream().map(envelope -> ((Transaction) envelope.payload()).orderRef().id()).toList();
    }

    private List<String> takenOffers() {
        return published.stream().map(envelope -> ((Transaction) envelope.payload()).offerRef().id()).toList();
    }

    @Test
    void isTheTeaPartyStoryListeningToOffersAndOrders() {
        assertThat(teaParty.name()).isEqualTo("tea-party");
        assertThat(teaParty.listensTo()).containsExactlyInAnyOrder(Offer.class, Order.class);
        assertThat(teaParty.playsFor()).hasValue(TTL);
    }

    @Test
    void settlesAnOrderWithAnOfferForItsThing() {
        teaParty.on(envelope(offer("OFF-1", TOP_HAT, 12, MARCH_HARE)));

        teaParty.on(envelope(order("ORD-1", ALICE, TOP_HAT)));

        assertThat(published).singleElement().satisfies(envelope -> {
            var transaction = (Transaction) envelope.payload();
            assertThat(transaction.customerId()).isEqualTo(ALICE);
            assertThat(transaction.sellerId()).isEqualTo(MARCH_HARE);
            assertThat(transaction.price()).isEqualTo(12);
            assertThat(transaction.orderRef().id()).isEqualTo("ORD-1");
            assertThat(transaction.offerRef().id()).isEqualTo("OFF-1");
        });
    }

    @Test
    void keepsAnOrderWaitingUntilItsThingIsOffered() {
        teaParty.on(envelope(order("ORD-1", ALICE, TOP_HAT)));
        assertThat(published).isEmpty();

        teaParty.on(envelope(offer("OFF-1", TOP_HAT, 10, MAD_HATTER)));

        assertThat(settledOrders()).containsExactly("ORD-1");
    }

    @Test
    void keepsAnOfferWaitingUntilItsThingIsOrdered() {
        teaParty.on(envelope(offer("OFF-1", TOP_HAT, 10, MAD_HATTER)));
        assertThat(published).isEmpty();

        teaParty.on(envelope(order("ORD-1", ALICE, TOP_HAT)));

        assertThat(takenOffers()).containsExactly("OFF-1");
    }

    /// One offer, one order: the second order waits for the next offer, and the tarts wait for their own.
    @Test
    void takesAnOfferOnceAndLetsTheNextOrderWait() {
        teaParty.on(envelope(order("ORD-1", ALICE, TOP_HAT)));
        teaParty.on(envelope(order("ORD-2", DORMOUSE, TOP_HAT)));
        teaParty.on(envelope(order("ORD-3", ALICE, TARTS)));

        teaParty.on(envelope(offer("OFF-1", TOP_HAT, 10, MAD_HATTER)));
        assertThat(settledOrders()).containsExactly("ORD-1");

        teaParty.on(envelope(offer("OFF-2", TOP_HAT, 11, MARCH_HARE)));
        assertThat(settledOrders()).containsExactly("ORD-1", "ORD-2");
    }

    /// The offer that waited longest goes first.
    @Test
    void servesWaitingOffersInTheOrderTheyCame() {
        teaParty.on(envelope(offer("OFF-1", TOP_HAT, 10, MAD_HATTER)));
        teaParty.on(envelope(offer("OFF-2", TOP_HAT, 11, MARCH_HARE)));

        teaParty.on(envelope(order("ORD-1", ALICE, TOP_HAT)));
        teaParty.on(envelope(order("ORD-2", DORMOUSE, TOP_HAT)));

        assertThat(takenOffers()).containsExactly("OFF-1", "OFF-2");
    }

    @Test
    void refusesAPayloadItDoesNotListenTo() {
        var envelope = envelope(product());

        assertThatIllegalStateException()
                .isThrownBy(() -> teaParty.on(envelope))
                .withMessageContaining("tea-party");
    }
}
