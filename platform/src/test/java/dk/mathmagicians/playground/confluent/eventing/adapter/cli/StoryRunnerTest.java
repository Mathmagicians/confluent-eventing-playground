package dk.mathmagicians.playground.confluent.eventing.adapter.cli;

import static org.assertj.core.api.Assertions.assertThat;

import dk.mathmagicians.playground.confluent.eventing.application.Stories;
import dk.mathmagicians.playground.confluent.eventing.application.Story;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;

class StoryRunnerTest {

    /// A story that counts its starts.
    private record Counting(String name, AtomicInteger starts) implements Story {

        @Override
        public void start() {
            starts.incrementAndGet();
        }
    }

    @Test
    void startsTheSelectedStoryOnce() {
        var selected = new Counting("selected", new AtomicInteger());
        var other = new Counting("other", new AtomicInteger());
        var runner = new StoryRunner(new Stories(List.of(other, selected), "selected"));

        runner.run(new DefaultApplicationArguments());

        assertThat(selected.starts()).hasValue(1);
        assertThat(other.starts()).hasValue(0);
    }
}
