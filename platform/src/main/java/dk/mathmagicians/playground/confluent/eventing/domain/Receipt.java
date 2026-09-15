package dk.mathmagicians.playground.confluent.eventing.domain;

import org.jmolecules.ddd.annotation.ValueObject;

/// Where a message landed: the envelope it carried, the topic, the partition, and the offset.
 @ValueObject
public record Receipt(String envelopeId, String topic, int partition, long offset) {
}
