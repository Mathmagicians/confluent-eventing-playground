package dk.mathmagicians.playground.confluent.eventing.domain;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;

class PayloadTest {


    static final Pattern ID_PATTERN = Pattern.compile(("(?<prefix>[^-]+)-(?<uuid>.+)"));

    static Stream<String> idIsAPrefixDashUUIDVersion3() {
        return Stream.of( offer().offerId(), order().id(), transaction().transactionId(), envelope(offer()).id());
    }
    @MethodSource
    @ParameterizedTest
    void idIsAPrefixDashUUIDVersion3(String id) {
        var matcher = ID_PATTERN.matcher(id);
        assertThat(matcher.matches()).isTrue();
        assertThat(matcher.group("prefix").length()).isBetween(1, 4);
        var uuid = UUID.fromString(matcher.group("uuid"));
        assertThat(uuid.version()).isEqualTo(3);
    }
}
