package dk.mathmagicians.playground.confluent.eventing.application;

import dk.mathmagicians.playground.confluent.eventing.domain.Envelope;
import dk.mathmagicians.playground.confluent.eventing.domain.Payload;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import org.jmolecules.architecture.hexagonal.PrimaryPort;
import org.jspecify.annotations.Nullable;

/// The substrate's contract with a business process. A story says what it listens to, and the substrate hands it
/// every message of those kinds; what it wants to publish, it publishes through `PublishMessage`. A story that
/// acts on its own starts when the substrate does. `--story=<name>` picks the one this process plays, `<name>.*`
/// is its settings, and `<name>` the consumer group it reads under, unless the story says otherwise.
@PrimaryPort
public interface Story {

    /// The name on the command line and the property prefix.
    String name();

    /// The consumer group the story reads under: its name, so the instances of a story share the partitions. A
    /// story that must see the whole stream, an aggregate over a key that is not the partition key, answers a
    /// group of its own per instance instead.
    default String group() {
        return name();
    }

    /// The payload types the story wants to see. Empty for a story that only publishes.
    default Set<Class<? extends Payload>> listensTo() {
        return Set.of();
    }

    /// A message of a kind the story listens to. The default is the refusal, for what a story does not listen to.
    default void on(Envelope envelope) {
        throw new IllegalStateException(name() + " does not listen to " + envelope.payload());
    }

    /// Once, when the substrate is up. A story that acts on its own does its work here.
    default void start() {
    }

    /// How long the story plays, its settings' `<name>.ttl`, counted from before `start()`: the substrate ends
    /// the process when it has passed. Empty, the default, is until stopped, what a consumer wants in production.
    default Optional<Duration> playsFor() {
        return Optional.empty();
    }

    /// The ttl as a story's settings read it, `<name>.ttl` as text: positive seconds, or left empty for until
    /// stopped, anything else fails the start with the reason. Text, because an empty value on the command line
    /// is the one way to say until stopped when the setting has a default.
    static Optional<Duration> ttl(String name, @Nullable String ttl) {
        if (ttl == null || ttl.isBlank()) {
            return Optional.empty();
        }
        var refused = new IllegalArgumentException(
                name + ".ttl must be positive seconds, left empty for until stopped, was '" + ttl + "'");
        long seconds;
        try {
            seconds = Long.parseLong(ttl.strip());
        } catch (NumberFormatException e) {
            throw refused;
        }
        if (seconds <= 0) {
            throw refused;
        }
        return Optional.of(Duration.ofSeconds(seconds));
    }
}
