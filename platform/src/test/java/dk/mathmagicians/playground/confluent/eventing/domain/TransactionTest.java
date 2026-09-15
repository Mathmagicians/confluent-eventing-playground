package dk.mathmagicians.playground.confluent.eventing.domain;

import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.ALICE;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.AT;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.MAD_HATTER;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.TARTS;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.TOP_HAT;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.offer;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.order;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.sample;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class TransactionTest {

    private static final Order ORDER = order("ORD-1", ALICE, TOP_HAT);
    private static final Offer OFFER = offer("OFF-1", TOP_HAT, 12.5, MAD_HATTER);

    static Stream<Transaction> transactions() {
        return sample(EventFixtures::transaction);
    }

    @Test
    void settlesAnOrderWithAnOfferForTheSameThing() {
        var transaction = Transaction.settle(ORDER, OFFER, AT);

        assertThat(transaction.orderRef()).isEqualTo(ORDER);
        assertThat(transaction.offerRef()).isEqualTo(OFFER);
        assertThat(transaction.customerId()).isEqualTo(ALICE);
        assertThat(transaction.sellerId()).isEqualTo(MAD_HATTER);
        assertThat(transaction.price()).isEqualTo(12.5);
        assertThat(transaction.createdAt()).isEqualTo(AT);
    }

    /// One settlement, one id: the order's and the offer's behind the prefix, the same however often it runs.
    @Test
    void namesTheSettlementAfterTheOrderAndTheOffer() {
        assertThat(Transaction.settle(ORDER, OFFER, AT).id()).isEqualTo("TX-ORD-1-OFF-1");
    }

    @Test
    void refusesToSettleAnOrderWithAnOfferForAnotherThing() {
        var tarts = offer("OFF-2", TARTS, 12.5, MAD_HATTER);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> Transaction.settle(ORDER, tarts, AT))
                .withMessageContaining(TOP_HAT)
                .withMessageContaining(TARTS);
    }

    @ParameterizedTest
    @MethodSource("transactions")
    void copiesCustomerSellerAndPriceFromItsRefs(Transaction transaction) {
        assertThat(transaction.customerId()).isEqualTo(transaction.orderRef().customerId());
        assertThat(transaction.sellerId()).isEqualTo(transaction.offerRef().sellerId());
        assertThat(transaction.price()).isEqualTo(transaction.offerRef().price());
    }
}
