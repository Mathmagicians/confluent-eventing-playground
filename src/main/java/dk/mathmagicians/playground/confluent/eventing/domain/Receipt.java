package dk.mathmagicians.playground.confluent.eventing.domain;

/// Where a message landed: the envelope it carried, the topic, the partition, and the offset.
public record Receipt(String envelopeId, String topic, int partition, long offset) {
}
