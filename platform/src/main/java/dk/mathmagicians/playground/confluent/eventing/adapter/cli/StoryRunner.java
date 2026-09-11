package dk.mathmagicians.playground.confluent.eventing.adapter.cli;

import dk.mathmagicians.playground.confluent.eventing.application.Stories;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/// Driving adapter: the command line. The process plays the story `--story` names, and this runner starts it once
/// the context is up. A story that acts on its own does its work here, and the process ends with it; a story that
/// listens is fed by the consumer adapter and lives on. A record: Spring injects the canonical constructor.
@PrimaryAdapter
@Component
public record StoryRunner(Stories stories) implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StoryRunner.class);

    @Override
    public void run(ApplicationArguments args) {
        var story = stories.selected();
        log.info("Playing {}, of {}", story.name(), stories.names());
        story.start();
    }
}
