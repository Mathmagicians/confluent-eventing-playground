package dk.mathmagicians.playground.confluent.stories.teaparty;

import dk.mathmagicians.playground.confluent.eventing.application.PublishMessage;
import dk.mathmagicians.playground.confluent.eventing.application.Publisher;
import java.time.Clock;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/// Joins a distribution as every story does, an auto-configuration listed in `META-INF/spring`: the settings
/// bound, the party wired from what the platform offers.
@AutoConfiguration
@EnableConfigurationProperties(TeaPartyProperties.class)
class TeaPartyStory {

    /// The tea party publishes its transactions for its region, named after `spring.application.name`, through
    /// the profile's publisher, drawing ids from the calling thread's random source.
    @Bean
    SettleAtTheTeaParty teaParty(TeaPartyProperties teaParty, Publisher publisher, Clock clock,
                                 @Value("${spring.application.name}") String app) {
        var publish = new PublishMessage(teaParty.region(), app, publisher, clock, ThreadLocalRandom::current);
        return new SettleAtTheTeaParty(
                teaParty.region(), publish, clock, ThreadLocalRandom::current, teaParty.ttl());
    }
}
