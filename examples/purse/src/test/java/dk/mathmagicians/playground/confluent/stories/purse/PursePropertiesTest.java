package dk.mathmagicians.playground.confluent.stories.purse;

import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.character;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import dk.mathmagicians.playground.confluent.stories.purse.PurseProperties.Opening;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class PursePropertiesTest {

    private static final Duration FOREVER = Duration.ZERO;

    @Test
    void opensOnePursePerEntryWithTheOwnersIdOnTheWire() {
        var settings = new PurseProperties(List.of("Alice:1000", "White Rabbit: 500.5"), FOREVER);

        assertThat(settings.openings())
                .containsExactly(
                        new Opening(character("Alice"), 1000),
                        new Opening(character("White Rabbit"), 500.5));
    }

    @Test
    void holdsTheTtl() {
        assertThat(new PurseProperties(List.of("Alice:1"), Duration.ofSeconds(30)).ttl())
                .isEqualTo(Duration.ofSeconds(30));
    }

    @Test
    void requiresAtLeastOnePurse() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new PurseProperties(List.of(), FOREVER))
                .withMessageContaining("purse.owners");
    }

    @Test
    void rejectsAnEntryWithoutCoins() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new PurseProperties(List.of("Alice"), FOREVER))
                .withMessageContaining("'Alice'")
                .withMessageContaining("Name:coins");
    }

    @Test
    void rejectsCoinsThatAreNoNumber() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new PurseProperties(List.of("Alice:many"), FOREVER))
                .withMessageContaining("'Alice:many'");
    }

    @Test
    void rejectsAStrangerByName() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new PurseProperties(List.of("Humpty Dumpty:10"), FOREVER))
                .withMessageContaining("Humpty Dumpty");
    }

    @Test
    void rejectsADebtToBeginWith() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new PurseProperties(List.of("Alice:-1"), FOREVER))
                .withMessageContaining("'Alice:-1'")
                .withMessageContaining("debt");
    }

    @Test
    void rejectsAnOwnerNamedTwice() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new PurseProperties(List.of("Alice:1", "Alice:2"), FOREVER))
                .withMessageContaining(character("Alice") + " twice");
    }

    @Test
    void rejectsANegativeTtl() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new PurseProperties(List.of("Alice:1"), Duration.ofSeconds(-1)))
                .withMessageContaining("purse.ttl");
    }
}
