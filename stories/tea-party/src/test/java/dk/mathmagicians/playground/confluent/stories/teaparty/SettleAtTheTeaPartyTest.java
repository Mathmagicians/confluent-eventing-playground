package dk.mathmagicians.playground.confluent.stories.teaparty;

import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.APP;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.AT;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.REGION;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.character;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.dice;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.envelope;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.product;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.publisher;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.thing;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import dk.mathmagicians.playground.confluent.eventing.application.PublishMessage;
import dk.mathmagicians.playground.confluent.eventing.domain.Envelope;
import dk.mathmagicians.playground.confluent.eventing.domain.Offer;
import dk.mathmagicians.playground.confluent.eventing.domain.Order;
import dk.mathmagicians.playground.confluent.eventing.domain.Transaction;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/// The tea party over offers and orders built by hand: what it publishes is what it settled.
class SettleAtTheTeaPartyTest {

    private static final Clock CLOCK = Clock.fixed(AT, ZoneOffset.UTC);
    private static final String TOP_HAT = thing("Top Hat");
    private static final String TARTS = thing("Tarts");
    private static final String ALICE = character("Alice");
    private static final String DORMOUSE = character("Dormouse");
    private static final String HATTER = character("Mad Hatter");
    private static final String HARE = character("March Hare");

    /// Envelopes as the tea party publishes them.
    private final List<Envelope> published = new ArrayList<>();
    private final SettleAtTheTeaParty teaParty = new SettleAtTheTeaParty(
            REGION,
            new PublishMessage(REGION, APP, publisher(published), CLOCK, () -> dice()),
            CLOCK,
            () -> dice());

    private static Envelope offered(String offerId, String productId, double price, String seller) {
        return envelope(new Offer(offerId, productId, price, seller, AT));
    }

    private static Envelope ordered(String orderId, String productId, String customer) {
        return envelope(new Order(orderId, customer, productId, AT));
    }

    private List<Transaction> settled() {
        return published.stream().map(envelope -> (Transaction) envelope.payload()).toList();
    }

    @Test
    void isTheTeaPartyStoryListeningToOffersAndOrders() {
        assertThat(teaParty.name()).isEqualTo("tea-party");
        assertThat(teaParty.listensTo()).containsExactlyInAnyOrder(Offer.class, Order.class);
    }

    @Test
    void settlesAnOrderAtTheLatestOfferForItsThing() {
        teaParty.on(offered("OFF-1", TOP_HAT, 10, HATTER));
        teaParty.on(offered("OFF-2", TOP_HAT, 12, HARE));

        teaParty.on(ordered("ORD-1", TOP_HAT, ALICE));

        assertThat(settled()).singleElement().satisfies(transaction -> {
            assertThat(transaction.customerId()).isEqualTo(ALICE);
            assertThat(transaction.sellerId()).isEqualTo(HARE);
            assertThat(transaction.price()).isEqualTo(12);
            assertThat(transaction.orderRef().id()).isEqualTo("ORD-1");
            assertThat(transaction.offerRef().offerId()).isEqualTo("OFF-2");
        });
    }

    @Test
    void keepsAnOrderWaitingUntilItsThingIsOffered() {
        teaParty.on(ordered("ORD-1", TOP_HAT, ALICE));
        assertThat(published).isEmpty();

        teaParty.on(offered("OFF-1", TOP_HAT, 10, HATTER));

        assertThat(settled()).singleElement().satisfies(transaction ->
                assertThat(transaction.orderRef().id()).isEqualTo("ORD-1"));
    }

    @Test
    void settlesEveryOrderThatWaitedForTheThingOldestFirst() {
        teaParty.on(ordered("ORD-1", TOP_HAT, ALICE));
        teaParty.on(ordered("ORD-2", TOP_HAT, DORMOUSE));
        teaParty.on(ordered("ORD-3", TARTS, ALICE));

        teaParty.on(offered("OFF-1", TOP_HAT, 10, HATTER));

        assertThat(settled()).extracting(transaction -> transaction.orderRef().id())
                .containsExactly("ORD-1", "ORD-2");
    }

    @Test
    void settlesOnceOnly() {
        teaParty.on(ordered("ORD-1", TOP_HAT, ALICE));
        teaParty.on(offered("OFF-1", TOP_HAT, 10, HATTER));

        teaParty.on(offered("OFF-2", TOP_HAT, 11, HARE));

        assertThat(settled()).hasSize(1);
    }

    @Test
    void refusesAPayloadItDoesNotListenTo() {
        var envelope = envelope(product());

        assertThatIllegalStateException()
                .isThrownBy(() -> teaParty.on(envelope))
                .withMessageContaining("tea-party");
    }
}
