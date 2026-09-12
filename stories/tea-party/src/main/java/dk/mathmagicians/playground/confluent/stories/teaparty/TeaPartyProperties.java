package dk.mathmagicians.playground.confluent.stories.teaparty;

import dk.mathmagicians.playground.confluent.eventing.application.Story;
import java.time.Duration;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/// The settings of the tea party story, `tea-party.*`: the region whose orders and offers it reads,
/// `--tea-party.region`, the platform's `region` unless given, and how long it sits, `--tea-party.ttl` seconds,
/// five minutes by default, left empty for until stopped. A wrong value fails startup.
@ConfigurationProperties("tea-party")
public record TeaPartyProperties(@Nullable String region, @DefaultValue("300") String ttl) {

    public TeaPartyProperties {
        if (region != null && region.isBlank()) {
            throw new IllegalArgumentException(
                    SettleAtTheTeaParty.NAME + ".region cannot be blank; leave it out for the platform's");
        }
        Story.ttl(SettleAtTheTeaParty.NAME, ttl);
    }

    /// How long the party sits, empty for until stopped.
    public Optional<Duration> playsFor() {
        return Story.ttl(SettleAtTheTeaParty.NAME, ttl);
    }
}
