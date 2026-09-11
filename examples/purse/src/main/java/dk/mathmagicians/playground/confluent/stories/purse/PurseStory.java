package dk.mathmagicians.playground.confluent.stories.purse;

import static java.util.stream.Collectors.toMap;

import dk.mathmagicians.playground.confluent.stories.purse.PurseProperties.Opening;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/// How the purse story joins a distribution: an auto-configuration Boot finds through `META-INF/spring`, the way
/// any jar adds its beans. It binds the story's settings and opens the purses.
@AutoConfiguration
@EnableConfigurationProperties(PurseProperties.class)
class PurseStory {

    @Bean
    KnowWhatIsLeft purse(PurseProperties purse) {
        return new KnowWhatIsLeft(purse.openings().stream().collect(toMap(Opening::ownerId, Opening::coins)));
    }
}
