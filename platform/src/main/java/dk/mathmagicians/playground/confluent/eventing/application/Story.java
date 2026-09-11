package dk.mathmagicians.playground.confluent.eventing.application;

import dk.mathmagicians.playground.confluent.eventing.domain.Envelope;
import dk.mathmagicians.playground.confluent.eventing.domain.Payload;
import java.util.Set;
import org.jmolecules.architecture.hexagonal.PrimaryPort;

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

    /// A message of a kind the story listens to.
    default void on(Envelope envelope) {
        throw new IllegalStateException(name() + " listens to nothing, got " + envelope.id());
    }

    /// Once, when the substrate is up. A story that acts on its own does its work here.
    default void start() {
    }
}
