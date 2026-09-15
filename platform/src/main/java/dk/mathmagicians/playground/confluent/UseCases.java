package dk.mathmagicians.playground.confluent;

import dk.mathmagicians.playground.confluent.eventing.application.Publisher;
import dk.mathmagicians.playground.confluent.eventing.application.Publishing;
import dk.mathmagicians.playground.confluent.eventing.application.Story;
import java.time.Clock;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/// The composition root of the substrate. Use cases are plain records without Spring, so their wiring is spelled
/// out: here what every story shares, in a story's auto-configuration the story itself.
@Configuration
class UseCases {

    /// Time is an input to the use cases; the system clock is wired once, here at the edge.
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    /// What a story publishes with: named after `spring.application.name`, for the platform's `region` unless
    /// the story has its own, through the profile's publisher, drawing ids from the calling thread's random source.
    @Bean
    Publishing publishing(Publisher publisher, Clock clock,
                          @Value("${spring.application.name}") String app, @Value("${region}") String region) {
        return new Publishing(app, region, publisher, clock, ThreadLocalRandom::current);
    }

    /// The story this process plays, `--story`, the one the adapters drive, among the ones the jars contribute.
    @Bean
    @Primary
    Story story(List<Story> stories, @Value("${story:}") String name) {
        return Story.named(name, stories);
    }
}
