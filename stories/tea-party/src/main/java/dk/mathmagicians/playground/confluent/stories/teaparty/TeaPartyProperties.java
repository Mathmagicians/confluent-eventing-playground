package dk.mathmagicians.playground.confluent.stories.teaparty;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.boot.convert.DurationUnit;

/// The settings of the tea party story, `tea-party.*`: the region whose orders and offers it reads,
/// `--tea-party.region`, and how long it sits, `--tea-party.ttl` seconds, five minutes by default, zero for until
/// stopped. Every value has a default, so the bundle binds at every start whatever the story; a wrong value fails
/// startup.
@ConfigurationProperties("tea-party")
public record TeaPartyProperties(
        @DefaultValue("EMEA") String region,
        @DefaultValue("300") @DurationUnit(ChronoUnit.SECONDS) Duration ttl) {

    public TeaPartyProperties {
        if (region == null || region.isBlank()) {
            throw new IllegalArgumentException("tea-party.region is required");
        }
        if (ttl == null || ttl.isNegative()) {
            throw new IllegalArgumentException("tea-party.ttl must be seconds, zero for until stopped, was " + ttl);
        }
    }
}
