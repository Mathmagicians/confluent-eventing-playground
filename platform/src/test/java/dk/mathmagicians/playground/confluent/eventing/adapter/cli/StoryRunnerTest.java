package dk.mathmagicians.playground.confluent.eventing.adapter.cli;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import dk.mathmagicians.playground.confluent.eventing.application.Stories;
import dk.mathmagicians.playground.confluent.eventing.application.Story;
import dk.mathmagicians.playground.confluent.eventing.domain.Order;
import dk.mathmagicians.playground.confluent.eventing.domain.Payload;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
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
    private static final Set<Class<? extends Payload>> ORDERS = Set.of(Order.class);
    private static final Set<Class<? extends Payload>> NOTHING = Set.of();

    /// A story that counts its starts, listens or not, and plays for the ttl given.
    private record Counting(String name, AtomicInteger starts, Set<Class<? extends Payload>> listensTo, Duration ttl)
            implements Story {

        Counting(String name, Set<Class<? extends Payload>> listensTo, Duration ttl) {
            this(name, new AtomicInteger(), listensTo, ttl);
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
        var selected = new Counting("selected", NOTHING, Duration.ZERO);
        var other = new Counting("other", NOTHING, Duration.ZERO);

        runner(selected, other).run(new DefaultApplicationArguments());

        assertThat(selected.starts()).hasValue(1);
        assertThat(other.starts()).hasValue(0);
    }

    @Test
    void leavesAConsumerWithoutATtlUntilStopped() throws InterruptedException {
        runner(new Counting("forever", ORDERS, Duration.ZERO)).run(new DefaultApplicationArguments());

        verify(context, never()).close();
    }

    @Test
    void leavesAStoryThatActsOnItsOwnToEndTheProcess() throws InterruptedException {
        runner(new Counting("load", NOTHING, SHORT)).run(new DefaultApplicationArguments());

        verify(context, never()).close();
    }

    @Test
    void closesTheContextOfAConsumerWhenItsTtlHasPassed() throws InterruptedException {
        var story = new Counting("brief", ORDERS, SHORT);
        var before = Instant.now();

        runner(story).run(new DefaultApplicationArguments());

        assertThat(Duration.between(before, Instant.now())).isGreaterThanOrEqualTo(SHORT);
        verify(context).close();
    }
}
