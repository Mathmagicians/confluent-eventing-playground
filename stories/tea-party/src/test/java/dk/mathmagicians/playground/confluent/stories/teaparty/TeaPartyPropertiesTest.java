package dk.mathmagicians.playground.confluent.stories.teaparty;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

class TeaPartyPropertiesTest {

    @Test
    void holdsTheRegion() {
        assertThat(new TeaPartyProperties("APAC").region()).isEqualTo("APAC");
    }

    @Test
    void requiresARegion() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new TeaPartyProperties(" "))
                .withMessageContaining("tea-party.region");
    }
}
