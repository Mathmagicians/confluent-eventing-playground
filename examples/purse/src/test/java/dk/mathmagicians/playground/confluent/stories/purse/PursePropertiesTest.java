package dk.mathmagicians.playground.confluent.stories.purse;

import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.character;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import dk.mathmagicians.playground.confluent.stories.purse.PurseProperties.Opening;
import java.util.List;
import org.junit.jupiter.api.Test;

class PursePropertiesTest {

    @Test
    void opensOnePursePerEntryWithTheOwnersIdOnTheWire() {
        var settings = new PurseProperties(List.of("Alice:1000", "White Rabbit: 500.5"));

        assertThat(settings.openings())
                .containsExactly(
                        new Opening(character("Alice"), 1000),
                        new Opening(character("White Rabbit"), 500.5));
    }

    @Test
    void requiresAtLeastOnePurse() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new PurseProperties(List.of()))
                .withMessageContaining("purse.owners");
    }

    @Test
    void rejectsAnEntryWithoutCoins() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new PurseProperties(List.of("Alice")))
                .withMessageContaining("'Alice'")
                .withMessageContaining("Name:coins");
    }

    @Test
    void rejectsCoinsThatAreNoNumber() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new PurseProperties(List.of("Alice:many")))
                .withMessageContaining("'Alice:many'");
    }

    @Test
    void rejectsAStrangerByName() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new PurseProperties(List.of("Humpty Dumpty:10")))
                .withMessageContaining("Humpty Dumpty");
    }

    @Test
    void rejectsADebtToBeginWith() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new PurseProperties(List.of("Alice:-1")))
                .withMessageContaining("'Alice:-1'")
                .withMessageContaining("debt");
    }

    @Test
    void rejectsAnOwnerNamedTwice() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new PurseProperties(List.of("Alice:1", "Alice:2")))
                .withMessageContaining(character("Alice") + " twice");
    }
}
