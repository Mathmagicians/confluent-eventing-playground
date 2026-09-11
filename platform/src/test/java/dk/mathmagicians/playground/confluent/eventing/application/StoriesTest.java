package dk.mathmagicians.playground.confluent.eventing.application;

import static dk.mathmagicians.playground.confluent.eventing.application.StoryFixtures.named;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.List;
import org.junit.jupiter.api.Test;

class StoriesTest {

    @Test
    void selectsTheStoryByName() {
        var stories = new Stories(List.of(named("load"), named("tea-party")), "tea-party");

        assertThat(stories.selected().name()).isEqualTo("tea-party");
        assertThat(stories.names()).containsExactly("load", "tea-party");
    }

    @Test
    void rejectsAnUnknownNameNamingTheKnownOnes() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Stories(List.of(named("load")), "purse"))
                .withMessageContaining("purse")
                .withMessageContaining("load");
    }

    @Test
    void rejectsTwoStoriesOfOneName() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Stories(List.of(named("load"), named("load")), "load"))
                .withMessageContaining("two stories are named load");
    }
}
