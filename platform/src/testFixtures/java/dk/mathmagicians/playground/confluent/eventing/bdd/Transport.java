package dk.mathmagicians.playground.confluent.eventing.bdd;

import dk.mathmagicians.playground.confluent.eventing.domain.Envelope;
import dk.mathmagicians.playground.confluent.eventing.domain.Payload;
import dk.mathmagicians.playground.confluent.eventing.domain.Receipt;
import dk.mathmagicians.playground.confluent.eventing.domain.Transaction;
import java.util.List;
import org.testcontainers.containers.GenericContainer;

/// Test driver port: how a scenario reaches the image under test on the suite's transport, one bean per transport,
/// picked by the profiles the `bdd` task gives the suite. The steps and the other drivers go through it and never
/// name a transport: on `confluent` it is the cluster and its registry, on `cli` the console a story reads and the
/// log the image writes.
public interface Transport {

    /// The scenario can connect to the transport.
    void assertOpen();

    /// Readies a story's container, before it starts, to listen under the group: what the scenario publishes from
    /// here on is what the story reads.
    void listen(GenericContainer<?> story, String group);

    /// The log line that says a started story listens, as a pattern.
    String listening();

    /// Whether the story under the group has read everything the streams of the payloads got since it started.
    boolean caughtUp(String group, List<String> payloads);

    /// The story under the group has left the table.
    void left(String group);

    /// Publishes the envelope as a story would, where the story at the table reads it, and answers the receipt.
    Receipt publish(Envelope envelope);

    /// Every transaction a story has published.
    List<Transaction> transactions();

    /// The payloads the receipts point at, read back from where they landed.
    List<Payload> payloads(List<Receipt> receipts);

    /// The message the receipt points at is on the topic and carries the envelope.
    void assertEnvelopeAt(String topic, Receipt receipt);

    /// The message the receipt points at is written with the Protobuf schema of the topic.
    void assertSerializedWithTheSchemaOf(String topic, Receipt receipt);

    /// The envelope the receipt points at names the region.
    void assertRegion(Receipt receipt, String region);
}
