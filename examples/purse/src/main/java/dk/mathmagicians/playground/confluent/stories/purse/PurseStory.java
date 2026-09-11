package dk.mathmagicians.playground.confluent.stories.purse;

import static java.util.stream.Collectors.toMap;

import dk.mathmagicians.playground.confluent.stories.purse.PurseProperties.Opening;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/// Joins a distribution as every story does, an auto-configuration listed in `META-INF/spring`: the settings
/// bound, the purses opened.
@AutoConfiguration
@EnableConfigurationProperties(PurseProperties.class)
class PurseStory {

    @Bean
    KnowWhatIsLeft purse(PurseProperties purse) {
        return new KnowWhatIsLeft(
                purse.openings().stream().collect(toMap(Opening::ownerId, Opening::coins)), purse.ttl());
    }
}
