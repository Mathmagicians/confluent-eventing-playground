package dk.mathmagicians.playground.confluent.stories.load;

import dk.mathmagicians.playground.confluent.eventing.domain.Offer;
import dk.mathmagicians.playground.confluent.eventing.domain.Order;
import dk.mathmagicians.playground.confluent.eventing.domain.Payload;
import dk.mathmagicians.playground.confluent.eventing.domain.Product;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.boot.convert.DurationUnit;

/// The settings of the load story, `load.*`: `--load.type`, `--load.concurrent` producers, each sleeping
/// `--load.interval` milliseconds between events, for `--load.region`, until `--load.ttl` seconds have passed.
/// Every value has a default, so the bundle binds at every start whatever the story; a wrong value fails
/// startup.
@ConfigurationProperties("load")
public record LoadProperties(
        @DefaultValue("offer") Type type,
        @DefaultValue("10") int concurrent,
        @DefaultValue("250") int interval,
        @DefaultValue("EMEA") String region,
        @DefaultValue("60") @DurationUnit(ChronoUnit.SECONDS) Duration ttl) {

    public static final Duration MAX_TTL = Duration.ofMinutes(5);

    public enum Type {
        OFFER,
        ORDER,
        PRODUCT
    }

    public LoadProperties {
        if (type == null) {
            throw new IllegalArgumentException("load.type is required: one of " + Arrays.toString(Type.values()));
        }
        if (concurrent <= 0) {
            throw new IllegalArgumentException("load.concurrent must be positive, was " + concurrent);
        }
        if (interval <= 0) {
            throw new IllegalArgumentException("load.interval must be positive milliseconds, was " + interval);
        }
        if (region == null || region.isBlank()) {
            throw new IllegalArgumentException("load.region is required");
        }
        if (ttl == null || ttl.isZero() || ttl.isNegative() || ttl.compareTo(MAX_TTL) > 0) {
            throw new IllegalArgumentException(
                    "load.ttl must be 1 to " + MAX_TTL.toSeconds() + " seconds, was " + ttl);
        }
    }

    /// The recipe of the type, the domain's random draw.
    public GenerateLoad.Recipe<? extends Payload> recipe() {
        return switch (type) {
            case PRODUCT -> Product::random;
            case OFFER -> Offer::random;
            case ORDER -> Order::random;
        };
    }
}
