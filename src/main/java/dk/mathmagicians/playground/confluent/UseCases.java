package dk.mathmagicians.playground.confluent;

import dk.mathmagicians.playground.confluent.eventing.adapter.cli.LoadProperties;
import dk.mathmagicians.playground.confluent.eventing.application.GenerateLoad;
import dk.mathmagicians.playground.confluent.eventing.application.PublishMessage;
import dk.mathmagicians.playground.confluent.eventing.application.Publisher;
import java.time.Clock;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/// The composition root. Use cases are plain records without Spring, so their wiring is spelled out here.
@Configuration
class UseCases {

    /// Time is an input to the use cases; the system clock is wired once, here at the edge.
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    /// A generator publishes for the region on its command line, named after `spring.application.name`, through
    /// the profile's publisher, drawing ids from the calling thread's random source.
    @Bean
    PublishMessage publishMessage(LoadProperties load, Publisher publisher, Clock clock,
                                  @Value("${spring.application.name}") String app) {
        return new PublishMessage(load.region(), app, publisher, clock, ThreadLocalRandom::current);
    }

    /// The load runs its producers through the publishing use case.
    @Bean
    GenerateLoad generateLoad(PublishMessage publishMessage, Clock clock) {
        return new GenerateLoad(publishMessage, clock);
    }
}
