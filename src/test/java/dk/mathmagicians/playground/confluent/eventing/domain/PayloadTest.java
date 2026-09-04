package dk.mathmagicians.playground.confluent.eventing.domain;

import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.sample;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class PayloadTest {

    static Stream<String> ids() {
        return sample((random, _) -> Payload.id("OF", random));
    }

    @ParameterizedTest
    @MethodSource("ids")
    void idIsThePrefixAndSixteenHexDigits(String id) {
        assertThat(id).matches("OF-[0-9a-f]{16}");
    }
}
