package dk.mathmagicians.playground.confluent.eventing.domain;

import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.sample;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class OfferTest {

    static Stream<Offer> offers() {
        return sample(Offer::random);
    }

    @ParameterizedTest
    @MethodSource("offers")
    void priceIsWithinTheBounds(Offer offer) {
        assertThat(offer.price()).isBetween(Offer.MIN_PRICE, Offer.MAX_PRICE);
    }

    @ParameterizedTest
    @MethodSource("offers")
    void priceIsWholeCents(Offer offer) {
        assertThat(BigDecimal.valueOf(offer.price()).scale()).isLessThanOrEqualTo(2);
    }
}
