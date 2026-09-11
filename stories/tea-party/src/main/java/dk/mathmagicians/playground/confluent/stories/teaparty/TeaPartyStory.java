package dk.mathmagicians.playground.confluent.stories.teaparty;

import dk.mathmagicians.playground.confluent.eventing.application.Publishing;
import java.time.Clock;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/// Joins a distribution as every story does, an auto-configuration listed in `META-INF/spring`: the settings
/// bound, the party wired from what the platform offers.
@AutoConfiguration
@EnableConfigurationProperties(TeaPartyProperties.class)
class TeaPartyStory {

    @Bean
    SettleAtTheTeaParty teaParty(TeaPartyProperties teaParty, Publishing publishing, Clock clock) {
        return new SettleAtTheTeaParty(
                teaParty.region(), publishing.from(teaParty.region()), clock, teaParty.playsFor().orElse(null));
    }
}
