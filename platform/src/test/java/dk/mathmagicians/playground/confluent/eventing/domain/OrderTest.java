package dk.mathmagicians.playground.confluent.eventing.domain;

import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.sample;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class OrderTest {

    static Stream<Order> orders() {
        return sample(Order::random);
    }

    @ParameterizedTest
    @MethodSource("orders")
    void isForOneOfTheThings(Order order) {
        assertThat(order.productId()).matches("P-[A-Z]{4}");
    }
}
