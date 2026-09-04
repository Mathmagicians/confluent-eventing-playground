package dk.mathmagicians.playground.confluent.eventing.load;

import dk.mathmagicians.playground.confluent.eventing.domain.Order;
import java.util.function.Supplier;

public final class OrderGenerator extends Generator<Order> {

    @Override
    protected Supplier<Order> supplier() {
        throw new UnsupportedOperationException("not implemented");
    }
}
