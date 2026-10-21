package dk.mathmagicians.playground.confluent.eventing.bdd;

import dk.mathmagicians.playground.confluent.eventing.domain.Envelope;
import dk.mathmagicians.playground.confluent.eventing.domain.Payload;
import dk.mathmagicians.playground.confluent.eventing.domain.Receipt;
import dk.mathmagicians.playground.confluent.eventing.domain.Transaction;
import io.cucumber.spring.ScenarioScope;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.testcontainers.containers.GenericContainer;

/// The transport on `confluent`: the Confluent test cluster and its registry, through `Cluster` and
/// `SchemaRegistry`. A story listens under its group, reading its topics from their end, and has read them when
/// the group's committed offsets reach the end of every partition that got records since it started. A record
/// a receipt points at is read once, for the steps that go on looking at it.
@Component
@ScenarioScope
@Profile("confluent")
class ClusterTransport implements Transport {

    /// Spring Kafka's line once the consumer has its partitions: from here on the story sees what is published
    private static final String LISTENING = ".*partitions assigned.*\\n";

    private final Cluster cluster;
    private final SchemaRegistry registry;
    private final Map<Receipt, ConsumerRecord<String, byte[]>> records = new HashMap<>();
    private Map<TopicPartition, Long> endAtStart = Map.of();

    ClusterTransport(Cluster cluster, SchemaRegistry registry) {
        this.cluster = cluster;
        this.registry = registry;
    }

    @Override
    public void assertOpen() {
        cluster.assertReachable();
    }

    @Override
    public void listen(GenericContainer<?> story, String group) {
        endAtStart = cluster.endOffsets(cluster.topics());
        story.setCommandParts(Stream.concat(
                        Stream.of(story.getCommandParts()),
                        Stream.of(
                                "--spring.kafka.consumer.group-id=" + group,
                                "--spring.kafka.consumer.auto-offset-reset=latest"))
                .toArray(String[]::new));
    }

    @Override
    public String listening() {
        return LISTENING;
    }

    @Override
    public boolean caughtUp(String group, List<String> payloads) {
        return cluster.caughtUp(group, payloads.stream().map(cluster::topic).toList(), endAtStart);
    }

    @Override
    public void left(String group) {
        cluster.forget(group);
    }

    @Override
    public Receipt publish(Envelope envelope) {
        return cluster.publish(envelope);
    }

    @Override
    public List<Transaction> transactions() {
        return cluster.transactions();
    }

    @Override
    public List<Payload> payloads(List<Receipt> receipts) {
        return cluster.payloads(receipts);
    }

    @Override
    public void assertEnvelopeAt(String topic, Receipt receipt) {
        records.put(receipt, cluster.assertEnvelopeAt(topic, receipt));
    }

    @Override
    public void assertSerializedWithTheSchemaOf(String topic, Receipt receipt) {
        registry.assertSerializedWithLatest(record(receipt).value(), topic);
    }

    @Override
    public void assertRegion(Receipt receipt, String region) {
        cluster.assertRegion(record(receipt), region);
    }

    private ConsumerRecord<String, byte[]> record(Receipt receipt) {
        return records.computeIfAbsent(receipt, at -> cluster.read(at.topic(), at.partition(), at.offset()));
    }
}
