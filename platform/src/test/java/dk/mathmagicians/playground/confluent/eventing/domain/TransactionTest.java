package dk.mathmagicians.playground.confluent.eventing.domain;

import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.AT;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.character;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.dice;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.sample;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.thing;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class TransactionTest {

    private static final String TOP_HAT = thing("Top Hat");
    private static final String TARTS = thing("Tarts");

    static Stream<Transaction> transactions() {
        return sample(Transaction::random);
    }

    @Test
    void settlesAnOrderWithAnOfferForTheSameThing() {
        var order = new Order("ORD-1", character("Alice"), TOP_HAT, AT);
        var offer = new Offer("OFF-1", TOP_HAT, 12.5, character("Mad Hatter"), AT);

        var transaction = Transaction.settle(order, offer, dice(), AT);

        assertThat(transaction.orderRef()).isEqualTo(order);
        assertThat(transaction.offerRef()).isEqualTo(offer);
        assertThat(transaction.customerId()).isEqualTo(character("Alice"));
        assertThat(transaction.sellerId()).isEqualTo(character("Mad Hatter"));
        assertThat(transaction.price()).isEqualTo(12.5);
        assertThat(transaction.transactionId()).startsWith("TX-");
    }

    @Test
    void refusesToSettleAnOrderWithAnOfferForAnotherThing() {
        var order = new Order("ORD-1", character("Alice"), TOP_HAT, AT);
        var offer = new Offer("OFF-1", TARTS, 12.5, character("Mad Hatter"), AT);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> Transaction.settle(order, offer, dice(), AT))
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
