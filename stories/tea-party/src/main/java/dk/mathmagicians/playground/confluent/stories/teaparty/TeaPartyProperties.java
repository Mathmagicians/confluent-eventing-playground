package dk.mathmagicians.playground.confluent.stories.teaparty;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/// The settings of the tea party story, `tea-party.*`: the region whose orders and offers it reads,
/// `--tea-party.region`. Every value has a default, so the bundle binds at every start whatever the story; a
/// wrong value fails startup.
@ConfigurationProperties("tea-party")
public record TeaPartyProperties(@DefaultValue("EMEA") String region) {

    public TeaPartyProperties {
        if (region == null || region.isBlank()) {
            throw new IllegalArgumentException("tea-party.region is required");
        }
    }
}
