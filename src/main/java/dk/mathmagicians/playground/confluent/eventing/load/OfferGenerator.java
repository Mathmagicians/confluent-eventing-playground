package dk.mathmagicians.playground.confluent.eventing.load;

import dk.mathmagicians.playground.confluent.eventing.domain.Offer;
import java.util.function.Supplier;

public final class OfferGenerator extends Generator<Offer> {

    @Override
    protected Supplier<Offer> supplier() {
        throw new UnsupportedOperationException("not implemented");
    }
}
