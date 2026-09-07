package dk.mathmagicians.playground.confluent;

import dk.mathmagicians.playground.confluent.eventing.application.GenerateLoad;
import dk.mathmagicians.playground.confluent.eventing.application.GenerateLoadService;
import dk.mathmagicians.playground.confluent.eventing.application.PublishMessage;
import dk.mathmagicians.playground.confluent.eventing.application.PublishMessageService;
import dk.mathmagicians.playground.confluent.eventing.application.Publisher;
import dk.mathmagicians.playground.confluent.eventing.adapter.cli.LoadProperties;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/// The composition root. Use cases are plain records without Spring, so their wiring is spelled out here.
@Configuration
class UseCases {

    /// The publisher is the profile's adapter, the log or Kafka.
    @Bean
    PublishMessage publishMessage(Publisher publisher) {
        return new PublishMessageService(publisher);
    }

    /// A generator stamps the region from its command line and the name of this application; the clock is wired
    /// here, at the edge.
    @Bean
    GenerateLoad generateLoad(LoadProperties load, PublishMessage publishMessage,
                              @Value("${spring.application.name}") String app) {
        return new GenerateLoadService(load.region(), app, publishMessage, Clock.systemUTC());
    }
}
