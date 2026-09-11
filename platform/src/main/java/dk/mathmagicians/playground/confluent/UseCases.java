package dk.mathmagicians.playground.confluent;

import dk.mathmagicians.playground.confluent.eventing.application.Stories;
import dk.mathmagicians.playground.confluent.eventing.application.Story;
import java.time.Clock;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/// The composition root of the substrate. Use cases are plain records without Spring, so their wiring is spelled
/// out: here what every story shares, in a story's auto-configuration the story itself.
@Configuration
class UseCases {

    /// Time is an input to the use cases; the system clock is wired once, here at the edge.
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    /// The stories on the classpath and the one this process plays, `--story`; a name nobody answers to fails
    /// the start with the names known.
    @Bean
    Stories stories(List<Story> stories, @Value("${story:}") String story) {
        return new Stories(stories, story);
    }
}
