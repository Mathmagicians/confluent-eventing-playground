package dk.mathmagicians.playground.confluent.eventing.domain;

import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.sample;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class TransactionTest {

    static Stream<Transaction> transactions() {
        return sample(Transaction::random);
    }

    @ParameterizedTest
    @MethodSource("transactions")
    void copiesCustomerSellerAndPriceFromItsRefs(Transaction transaction) {
        assertThat(transaction.customerId()).isEqualTo(transaction.orderRef().customerId());
        assertThat(transaction.sellerId()).isEqualTo(transaction.offerRef().sellerId());
        assertThat(transaction.price()).isEqualTo(transaction.offerRef().price());
    }
}
