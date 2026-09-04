package dk.mathmagicians.playground.confluent.eventing.domain;

import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.AT;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.sample;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class OrderTest {

    static Stream<Order> orders() {
        return sample(Order::random);
    }

    @ParameterizedTest
    @MethodSource("orders")
    void hasOneToMaxItems(Order order) {
        assertThat(order.products()).hasSizeBetween(1, Order.MAX_ITEMS);
    }

    @Test
    void productsAreImmutable() {
        var order = new Order("O-1", "Alice", new ArrayList<>(List.of("P-POCK")), AT);

        assertThatThrownBy(() -> order.products().add("P-TEAS")).isInstanceOf(UnsupportedOperationException.class);
    }
}
