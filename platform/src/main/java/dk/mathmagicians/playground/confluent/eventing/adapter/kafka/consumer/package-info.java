/// Driving adapter: Confluent Cloud, where the messages a story listens to come from, for the test and prod
/// profiles.
@PrimaryAdapter
@Module(name = "Kafka Consumer")
package dk.mathmagicians.playground.confluent.eventing.adapter.kafka.consumer;

import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.jmolecules.ddd.annotation.Module;
