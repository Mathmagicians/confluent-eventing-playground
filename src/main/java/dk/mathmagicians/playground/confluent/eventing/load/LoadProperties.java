package dk.mathmagicians.playground.confluent.eventing.load;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;

/// What one load generator produces and how: `--load.type`, `--load.concurrent` producers, each sleeping
/// `--load.interval` milliseconds between events, for `--load.region`, until `--load.ttl` seconds have passed.
/// A wrong value fails startup.
@ConfigurationProperties("load")
public record LoadProperties(
        Type type,
        int concurrent,
        int interval,
        String region,
        @DurationUnit(ChronoUnit.SECONDS) Duration ttl) {

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
}
