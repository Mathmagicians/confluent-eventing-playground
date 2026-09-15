package dk.mathmagicians.playground.confluent.stories.load;

import dk.mathmagicians.playground.confluent.eventing.application.Publishing;
import java.time.Clock;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/// Joins a distribution as every story does, an auto-configuration listed in `META-INF/spring`: the settings
/// bound, the load wired from what the platform offers.
@AutoConfiguration
@EnableConfigurationProperties(LoadProperties.class)
class LoadStory {

    @Bean
    GenerateLoad load(LoadProperties load, Publishing publishing, Clock clock) {
        return new GenerateLoad(
                publishing.from(load.region()), clock, load.recipe(), load.concurrent(), load.interval(), load.ttl());
    }
}
