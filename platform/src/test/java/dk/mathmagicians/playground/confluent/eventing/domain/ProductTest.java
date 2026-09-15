package dk.mathmagicians.playground.confluent.eventing.domain;

import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.sample;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ProductTest {

    static Stream<Product> products() {
        return sample(Product::random);
    }

    @Test
    void idIsTheFirstFourLettersUpperCaseWithoutSpaces() {
        assertThat(Product.id("Pocket Watch")).isEqualTo("P-POCK");
    }

    @Test
    void idPadsAShortNameWithItsLastLetter() {
        assertThat(Product.id("Fan")).isEqualTo("P-FANN");
    }

    @ParameterizedTest
    @MethodSource("products")
    void descriptionNamesTheThing(Product product) {
        assertThat(product.productDescription()).contains(product.productName());
    }
}
