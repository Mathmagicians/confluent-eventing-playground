package dk.mathmagicians.playground.confluent.eventing.adapter.cli.boot;

import dk.mathmagicians.playground.confluent.eventing.application.Story;
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
/// the context is up. A story that acts on its own ends the process when its start returns. A story that listens
/// is kept alive by its consumer: with a ttl, counted from before its start, the runner waits for what is left of
/// it after the start returns, then closes the context, which stops the consumer and ends the process; without
/// one it lives on until stopped. A record: Spring injects the canonical constructor.
@PrimaryAdapter
@Component
public record StoryRunner(Story story, Clock clock, ConfigurableApplicationContext context)
        implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StoryRunner.class);

    @Override
    public void run(ApplicationArguments args) throws InterruptedException {
        var consumer = !story.listensTo().isEmpty();
        var ttl = story.playsFor();
        if (ttl.isEmpty() || !consumer) {
            log.info("Playing {}{}", story.name(), consumer ? " until stopped" : "");
            story.start();
            return;
        }
        var deadline = clock.instant().plus(ttl.get());
        log.info("Playing {} until {}", story.name(), deadline);
        story.start();
        var left = Duration.between(clock.instant(), deadline);
        if (left.isPositive()) {
            Thread.sleep(left);
        }
        log.info("{} has played its {}, closing", story.name(), ttl.get());
        context.close();
    }
}
