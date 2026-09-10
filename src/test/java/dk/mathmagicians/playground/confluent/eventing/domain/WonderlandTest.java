package dk.mathmagicians.playground.confluent.eventing.domain;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class WonderlandTest {

    @Test
    void characterIdIsTheNameInUpperSnakeCase() {
        assertThat(Wonderland.characterId("Mad Hatter")).isEqualTo("MAD_HATTER");
        assertThat(Wonderland.characterId("Alice")).isEqualTo("ALICE");
    }

    @Test
    void characterIdRejectsAStrangerByName() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Wonderland.characterId("Humpty Dumpty"))
                .withMessageContaining("Humpty Dumpty");
    }

    @ParameterizedTest
    @EnumSource(Wonderland.class)
    void nextReturnsAWord(Wonderland kind) {
        assertThat(kind.next(EventFixtures.dice())).isNotBlank();
    }

    @ParameterizedTest
    @EnumSource(Wonderland.class)
    void nextIsTheSameForTheSameSeed(Wonderland kind) {
        assertThat(kind.next(EventFixtures.dice())).isEqualTo(kind.next(EventFixtures.dice()));
    }
}
