package dk.mathmagicians.playground.confluent.eventing.domain;

/// Outbound port: where envelopes go. One adapter per profile, the log or Kafka.
@FunctionalInterface
public interface Publisher {

    void publish(Envelope envelope);
}
