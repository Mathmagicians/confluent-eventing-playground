package dk.mathmagicians.playground.confluent.eventing.adapter.cli;

import static dk.mathmagicians.playground.confluent.eventing.application.StoryFixtures.acting;
import static dk.mathmagicians.playground.confluent.eventing.application.StoryFixtures.listening;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import dk.mathmagicians.playground.confluent.eventing.application.Stories;
import dk.mathmagicians.playground.confluent.eventing.application.Story;
import dk.mathmagicians.playground.confluent.eventing.domain.Order;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.context.ConfigurableApplicationContext;

@ExtendWith(MockitoExtension.class)
class StoryRunnerTest {

    private static final Duration SHORT = Duration.ofMillis(20);

    @Mock
    private ConfigurableApplicationContext context;

    private final Clock clock = Clock.systemUTC();

    private StoryRunner runner(Story... stories) {
        return new StoryRunner(new Stories(List.of(stories), stories[0].name()), clock, context);
    }

    @Test
    void startsTheSelectedStoryOnce() throws InterruptedException {
        var selected = acting("selected", null);
        var other = acting("other", null);

        runner(selected, other).run(new DefaultApplicationArguments());

        assertThat(selected.starts()).hasValue(1);
        assertThat(other.starts()).hasValue(0);
    }

    @Test
    void leavesAConsumerWithoutATtlUntilStopped() throws InterruptedException {
        runner(listening("forever", Set.of(Order.class), null)).run(new DefaultApplicationArguments());

        verify(context, never()).close();
    }

    @Test
    void leavesAStoryThatActsOnItsOwnToEndTheProcess() throws InterruptedException {
        runner(acting("load", SHORT)).run(new DefaultApplicationArguments());

        verify(context, never()).close();
    }

    @Test
    void closesTheContextOfAConsumerWhenItsTtlHasPassed() throws InterruptedException {
        var before = Instant.now();

        runner(listening("brief", Set.of(Order.class), SHORT)).run(new DefaultApplicationArguments());

        assertThat(Duration.between(before, Instant.now())).isGreaterThanOrEqualTo(SHORT);
        verify(context).close();
    }
}
