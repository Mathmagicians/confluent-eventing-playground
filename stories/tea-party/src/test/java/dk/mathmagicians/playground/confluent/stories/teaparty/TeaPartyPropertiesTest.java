package dk.mathmagicians.playground.confluent.stories.teaparty;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class TeaPartyPropertiesTest {

    @Test
    void holdsTheRegionAndHowLongItSits() {
        var settings = new TeaPartyProperties("APAC", "30");

        assertThat(settings.region()).isEqualTo("APAC");
        assertThat(settings.playsFor()).hasValue(Duration.ofSeconds(30));
    }

    @Test
    void sitsUntilStoppedWhenTheTtlIsLeftEmpty() {
        assertThat(new TeaPartyProperties("APAC", "").playsFor()).isEmpty();
    }

    @Test
    void requiresARegion() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new TeaPartyProperties(" ", "30"))
                .withMessageContaining("tea-party.region");
    }

    @Test
    void rejectsATtlOfZero() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new TeaPartyProperties("EMEA", "0"))
                .withMessageContaining("tea-party.ttl");
    }
}
