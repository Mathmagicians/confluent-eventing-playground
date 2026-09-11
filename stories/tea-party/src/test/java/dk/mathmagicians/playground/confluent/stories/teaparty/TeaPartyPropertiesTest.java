package dk.mathmagicians.playground.confluent.stories.teaparty;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class TeaPartyPropertiesTest {

    private static final Duration FOREVER = Duration.ZERO;

    @Test
    void holdsTheRegionAndTheTtl() {
        var settings = new TeaPartyProperties("APAC", Duration.ofSeconds(30));

        assertThat(settings.region()).isEqualTo("APAC");
        assertThat(settings.ttl()).isEqualTo(Duration.ofSeconds(30));
    }

    @Test
    void requiresARegion() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new TeaPartyProperties(" ", FOREVER))
                .withMessageContaining("tea-party.region");
    }

    @Test
    void rejectsANegativeTtl() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new TeaPartyProperties("EMEA", Duration.ofSeconds(-1)))
                .withMessageContaining("tea-party.ttl");
    }
}
