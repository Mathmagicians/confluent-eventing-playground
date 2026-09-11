package dk.mathmagicians.playground.confluent.stories.purse;

import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.ALICE;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.WHITE_RABBIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import dk.mathmagicians.playground.confluent.stories.purse.PurseProperties.Opening;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class PursePropertiesTest {

    private static final String FOREVER = "";

    @Test
    void opensOnePursePerEntryWithTheOwnersIdOnTheWire() {
        var settings = new PurseProperties(List.of("Alice:1000", "White Rabbit: 500.5"), FOREVER);

        assertThat(settings.openings())
                .containsExactly(
                        new Opening(ALICE, 1000),
                        new Opening(WHITE_RABBIT, 500.5));
    }

    @Test
    void sitsForTheTtlInSeconds() {
        assertThat(new PurseProperties(List.of("Alice:1"), "30").playsFor()).hasValue(Duration.ofSeconds(30));
    }

    @Test
    void sitsUntilStoppedWhenTheTtlIsLeftEmpty() {
        assertThat(new PurseProperties(List.of("Alice:1"), FOREVER).playsFor()).isEmpty();
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
                .withMessageContaining(ALICE + " twice");
    }

    @Test
    void rejectsATtlOfZero() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new PurseProperties(List.of("Alice:1"), "0"))
                .withMessageContaining("purse.ttl");
    }
}
