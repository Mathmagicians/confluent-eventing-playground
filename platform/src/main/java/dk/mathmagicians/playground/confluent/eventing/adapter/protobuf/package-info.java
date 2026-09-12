/// The wire's translation, shared at the rim: the payload records and their Protobuf messages, each way. The
/// Kafka publisher and consumer put the messages on the wire and take them off, the console types them as text.
@Adapter
@Module(name = "Protobuf")
package dk.mathmagicians.playground.confluent.eventing.adapter.protobuf;

import org.jmolecules.architecture.hexagonal.Adapter;
import org.jmolecules.ddd.annotation.Module;
