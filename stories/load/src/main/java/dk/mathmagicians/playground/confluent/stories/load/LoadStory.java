package dk.mathmagicians.playground.confluent.stories.load;

import dk.mathmagicians.playground.confluent.eventing.application.PublishMessage;
import dk.mathmagicians.playground.confluent.eventing.application.Publisher;
import java.time.Clock;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/// Joins a distribution as every story does, an auto-configuration listed in `META-INF/spring`: the settings
/// bound, the load wired from what the platform offers.
@AutoConfiguration
@EnableConfigurationProperties(LoadProperties.class)
class LoadStory {

    /// The load story publishes for the region of its settings, named after `spring.application.name`, through
    /// the profile's publisher, drawing ids from the calling thread's random source.
    @Bean
    GenerateLoad load(LoadProperties load, Publisher publisher, Clock clock,
                      @Value("${spring.application.name}") String app) {
        var publish = new PublishMessage(load.region(), app, publisher, clock, ThreadLocalRandom::current);
        return new GenerateLoad(publish, clock, load.recipe(), load.concurrent(), load.interval(), load.ttl());
    }
}
