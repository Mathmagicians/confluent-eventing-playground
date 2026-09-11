package dk.mathmagicians.playground.confluent.eventing.adapter.cli;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import dk.mathmagicians.playground.confluent.eventing.application.Stories;
import dk.mathmagicians.playground.confluent.eventing.application.Story;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.context.ConfigurableApplicationContext;

@ExtendWith(MockitoExtension.class)
class StoryRunnerTest {

    private static final Duration SHORT = Duration.ofMillis(20);

    /// A story that counts its starts and plays for the ttl given.
    private record Counting(String name, AtomicInteger starts, Duration ttl) implements Story {

        Counting(String name) {
            this(name, new AtomicInteger(), Duration.ZERO);
        }

        @Override
        public void start() {
            starts.incrementAndGet();
        }
    }

    @Mock
    private ConfigurableApplicationContext context;

    private final Clock clock = Clock.systemUTC();

    private StoryRunner runner(Story... stories) {
        return new StoryRunner(new Stories(List.of(stories), stories[0].name()), clock, context);
    }

    @Test
    void startsTheSelectedStoryOnce() throws InterruptedException {
        var selected = new Counting("selected");
        var other = new Counting("other");

        runner(selected, other).run(new DefaultApplicationArguments());

        assertThat(selected.starts()).hasValue(1);
        assertThat(other.starts()).hasValue(0);
    }

    @Test
    void leavesAStoryWithoutATtlToItself() throws InterruptedException {
        runner(new Counting("forever")).run(new DefaultApplicationArguments());

        verify(context, never()).close();
    }

    @Test
    void closesTheContextWhenTheTtlHasPassed() throws InterruptedException {
        var story = new Counting("brief", new AtomicInteger(), SHORT);
        var before = Instant.now();

        runner(story).run(new DefaultApplicationArguments());

        assertThat(Duration.between(before, Instant.now())).isGreaterThanOrEqualTo(SHORT);
        verify(context).close();
    }
}
