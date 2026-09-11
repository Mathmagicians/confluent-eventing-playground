package dk.mathmagicians.playground.confluent.stories.load;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dk.mathmagicians.playground.confluent.stories.load.LoadProperties.Type;
import dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures;
import dk.mathmagicians.playground.confluent.eventing.domain.Order;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class LoadPropertiesTest {

    private static final Duration TTL = Duration.ofSeconds(60);

    @Test
    void holdsAValidLoad() {
        var load = new LoadProperties(Type.OFFER, 10, 250, "EMEA", TTL);

        assertThat(load).isEqualTo(new LoadProperties(Type.OFFER, 10, 250, "EMEA", TTL));
    }

    @Test
    void answersTheRecipeOfItsType() {
        var orders = new LoadProperties(Type.ORDER, 10, 250, "EMEA", TTL);

        assertThat(orders.recipe().from(EventFixtures.dice(), EventFixtures.AT)).isInstanceOf(Order.class);
    }

    @Test
    void requiresAType() {
        assertThatThrownBy(() -> new LoadProperties(null, 10, 250, "EMEA", TTL))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("load.type")
                .hasMessageContaining("OFFER");
    }

    @Test
    void rejectsZeroProducers() {
        assertThatThrownBy(() -> new LoadProperties(Type.OFFER, 0, 250, "EMEA", TTL))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("load.concurrent")
                .hasMessageContaining("was 0");
    }

    @Test
    void rejectsANegativeInterval() {
        assertThatThrownBy(() -> new LoadProperties(Type.OFFER, 10, -1, "EMEA", TTL))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("load.interval")
                .hasMessageContaining("was -1");
    }

    @Test
    void requiresARegion() {
        assertThatThrownBy(() -> new LoadProperties(Type.OFFER, 10, 250, " ", TTL))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("load.region");
    }

    @Test
    void rejectsAZeroTtl() {
        assertThatThrownBy(() -> new LoadProperties(Type.OFFER, 10, 250, "EMEA", Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("load.ttl")
                .hasMessageContaining("was PT0S");
    }

    @Test
    void rejectsATtlBeyondTheMaximum() {
        var beyond = LoadProperties.MAX_TTL.plusSeconds(1);

        assertThatThrownBy(() -> new LoadProperties(Type.OFFER, 10, 250, "EMEA", beyond))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("load.ttl must be 1 to 300 seconds")
                .hasMessageContaining("was " + beyond);
    }
}
