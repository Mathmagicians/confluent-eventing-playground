package dk.mathmagicians.playground.confluent.eventing.adapter.cli;

import dk.mathmagicians.playground.confluent.eventing.application.Stories;
import java.time.Clock;
import java.time.Duration;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

/// Driving adapter: the command line. The process plays the story `--story` names, and this runner starts it once
/// the context is up. A story with a ttl plays that long, counted from before its start: the runner waits for what
/// is left of it after the start returns, then closes the context, which stops the consumer and ends the process.
/// Without one, a story that acts on its own ends the process when its start returns, and a story that listens
/// lives on until stopped. A record: Spring injects the canonical constructor.
@PrimaryAdapter
@Component
public record StoryRunner(Stories stories, Clock clock, ConfigurableApplicationContext context)
        implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StoryRunner.class);

    @Override
    public void run(ApplicationArguments args) throws InterruptedException {
        var story = stories.selected();
        var ttl = story.ttl();
        if (ttl.isZero()) {
            log.info("Playing {} until stopped, of {}", story.name(), stories.names());
            story.start();
            return;
        }
        var deadline = clock.instant().plus(ttl);
        log.info("Playing {} until {}, of {}", story.name(), deadline, stories.names());
        story.start();
        var left = Duration.between(clock.instant(), deadline);
        if (left.isPositive()) {
            Thread.sleep(left);
        }
        log.info("{} has played its {}, closing", story.name(), ttl);
        context.close();
    }
}
