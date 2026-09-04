package dk.mathmagicians.playground.confluent.eventing.load;

import dk.mathmagicians.playground.confluent.eventing.domain.Product;
import java.util.function.Supplier;

public final class ProductGenerator extends Generator<Product> {

    @Override
    protected Supplier<Product> supplier() {
        throw new UnsupportedOperationException("not implemented");
    }
}
