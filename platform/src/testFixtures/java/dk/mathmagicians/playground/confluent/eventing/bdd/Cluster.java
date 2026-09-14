package dk.mathmagicians.playground.confluent.eventing.bdd;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.protobuf.DynamicMessage;
import com.google.protobuf.Message;
import dk.mathmagicians.playground.confluent.eventing.adapter.kafka.EnvelopeHeaders;
import dk.mathmagicians.playground.confluent.eventing.adapter.protobuf.Converter;
import dk.mathmagicians.playground.confluent.eventing.domain.Envelope;
import dk.mathmagicians.playground.confluent.eventing.domain.Payload;
import dk.mathmagicians.playground.confluent.eventing.domain.Receipt;
import dk.mathmagicians.playground.confluent.eventing.domain.Transaction;
import io.confluent.kafka.serializers.protobuf.KafkaProtobufDeserializer;
import io.confluent.kafka.serializers.protobuf.KafkaProtobufDeserializerConfig;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.GroupIdNotFoundException;
import org.apache.kafka.common.errors.GroupNotEmptyException;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.awaitility.Awaitility;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.core.env.Environment;

/// Test driver for the Confluent test cluster: one admin client from the `kafka` profile for the life of the
/// suite's context, a consumer per read, values as the raw bytes on the wire, and a producer per publish, for
/// a scenario that stands in for a story.
public class Cluster {

    /// Kafka's own request timeout; the first call pays for DNS, TLS, and SASL
    private static final long TIMEOUT_SECONDS = 30;

    private final KafkaProperties properties;
    private final Environment environment;
    private final AdminClient client;

    Cluster(KafkaProperties properties, Environment environment) {
        this.properties = properties;
        this.environment = environment;
        this.client = AdminClient.create(properties.buildAdminProperties());
    }

    /// The topic of a payload type on the cluster, `orders` as `test.orders`, from the profile's `topics.*`.
    public String topic(String payloads) {
        return environment.getRequiredProperty("topics." + payloads);
    }

    /// Forgets a consumer group's offsets, so the next story under that group starts afresh; a group the cluster
    /// never saw is nothing to forget. A group whose last member is still leaving, a story stopped a moment ago,
    /// is asked again until the session timeout has let it go.
    public void forget(String group) {
        Awaitility.await().atMost(Duration.ofSeconds(60))
                .pollInterval(Duration.ofSeconds(2))
                .ignoreException(GroupNotEmptyException.class)
                .untilAsserted(() -> {
                    try {
                        client.deleteConsumerGroups(List.of(group)).all().get(TIMEOUT_SECONDS, SECONDS);
                    } catch (ExecutionException e) {
                        switch (e.getCause()) {
                            case GroupIdNotFoundException _ -> {}
                            case GroupNotEmptyException notEmpty -> throw notEmpty;
                            default -> throw new IllegalStateException(
                                    "the cluster refused: " + e.getCause().getMessage(), e);
                        }
                    } catch (TimeoutException e) {
                        throw new IllegalStateException(
                                "the cluster did not answer within " + TIMEOUT_SECONDS + " s", e);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException("interrupted while asking the cluster", e);
                    }
                });
    }

    /// Publishes the envelope as a story would, headers and message on the payload's topic, and answers the
    /// receipt.
    public Receipt publish(Envelope envelope) {
        var message = Converter.to(envelope.payload());
        var topic = topic(envelope.payload().getClass().getSimpleName().toLowerCase() + "s");
        var record = new ProducerRecord<String, Message>(
                topic, null, envelope.key(), message, EnvelopeHeaders.headers(envelope, message));
        try (var producer = new KafkaProducer<String, Message>(properties.buildProducerProperties())) {
            var landed = producer.send(record).get(TIMEOUT_SECONDS, SECONDS);
            return new Receipt(envelope.id(), landed.topic(), landed.partition(), landed.offset());
        } catch (ExecutionException e) {
            throw new IllegalStateException("the cluster refused: " + e.getCause().getMessage(), e);
        } catch (TimeoutException e) {
            throw new IllegalStateException("the cluster did not answer within " + TIMEOUT_SECONDS + " s", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted while publishing", e);
        }
    }

    /// The payload the receipt points at, read back from the topic.
    public Payload payload(Receipt receipt) {
        return Converter.from(value(read(receipt.topic(), receipt.partition(), receipt.offset())));
    }

    /// Every transaction on the transactions topic, from the beginning.
    public List<Transaction> transactions() {
        return readAll(topic("transactions")).stream()
                .map(record -> Converter.from(value(record)))
                .filter(Transaction.class::isInstance)
                .map(Transaction.class::cast)
                .toList();
    }

    /// The API keys open the cluster: the cheapest authenticated call answers with the cluster id.
    void assertReachable() {
        assertThat(await(client.describeCluster().clusterId())).isNotBlank();
    }

    /// The topic is on the cluster, by its full name: `test.orders`.
    void assertTopicExists(String topic) {
        assertThat(await(client.listTopics().names())).contains(topic);
    }

    /// The record the receipt points at is on the topic and carries the envelope in its headers. Answers the
    /// record, for the steps that go on looking at it.
    ConsumerRecord<String, byte[]> assertEnvelopeAt(String topic, Receipt receipt) {
        assertThat(receipt.topic()).isEqualTo(topic);
        var record = read(topic, receipt.partition(), receipt.offset());

        assertThat(header(record, EnvelopeHeaders.ID)).isEqualTo(receipt.envelopeId());
        return record;
    }

    /// The envelope in the record's headers names the region.
    void assertRegion(ConsumerRecord<String, byte[]> record, String region) {
        assertThat(header(record, EnvelopeHeaders.REGION)).isEqualTo(region);
    }

    @PreDestroy
    void close() {
        client.close();
    }

    /// The value of a record as its message, resolved through the registry like a consumer would.
    <T extends Message> T value(ConsumerRecord<String, byte[]> record, Class<T> type) {
        var configs = new HashMap<String, Object>(properties.getProperties());
        configs.put(KafkaProtobufDeserializerConfig.SPECIFIC_PROTOBUF_VALUE_TYPE, type.getName());
        try (var deserializer = new KafkaProtobufDeserializer<T>()) {
            deserializer.configure(configs, false);
            return deserializer.deserialize(record.topic(), record.value());
        }
    }

    /// The value of a record as the message its registered schema describes, for a schema with no generated code:
    /// a table Flink registered.
    DynamicMessage value(ConsumerRecord<String, byte[]> record) {
        try (var deserializer = new KafkaProtobufDeserializer<DynamicMessage>()) {
            deserializer.configure(new HashMap<>(properties.getProperties()), false);
            return deserializer.deserialize(record.topic(), record.value());
        }
    }

    /// The record at the offset, headers and raw value.
    ConsumerRecord<String, byte[]> read(String topic, int partition, long offset) {
        var at = new TopicPartition(topic, partition);
        try (var consumer = new KafkaConsumer<String, byte[]>(rawConsumer())) {
            consumer.assign(List.of(at));
            consumer.seek(at, offset);
            return consumer.poll(Duration.ofSeconds(TIMEOUT_SECONDS)).records(at).stream()
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("no record at " + at + " offset " + offset));
        }
    }

    /// Every record of the topic from the beginning, in offset order within a partition, headers and raw value:
    /// what a compacted topic holds, a table's rows among them.
    List<ConsumerRecord<String, byte[]>> readAll(String topic) {
        try (var consumer = new KafkaConsumer<String, byte[]>(rawConsumer())) {
            var partitions = consumer.partitionsFor(topic).stream()
                    .map(info -> new TopicPartition(topic, info.partition()))
                    .toList();
            consumer.assign(partitions);
            consumer.seekToBeginning(partitions);
            var end = consumer.endOffsets(partitions);
            var deadline = Instant.now().plusSeconds(TIMEOUT_SECONDS);
            var records = new ArrayList<ConsumerRecord<String, byte[]>>();
            while (partitions.stream().anyMatch(partition -> consumer.position(partition) < end.get(partition))) {
                assertThat(Instant.now()).as("reading %s to its end", topic).isBefore(deadline);
                consumer.poll(Duration.ofSeconds(1)).forEach(records::add);
            }
            return records;
        }
    }

    /// a consumer of the `kafka` profile that leaves keys as strings and values as the bytes on the wire
    private HashMap<String, Object> rawConsumer() {
        var configs = new HashMap<String, Object>(properties.buildConsumerProperties());
        configs.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configs.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
        configs.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        return configs;
    }

    private static String header(ConsumerRecord<String, byte[]> record, String name) {
        var header = record.headers().lastHeader(name);
        assertThat(header).as("header %s", name).isNotNull();
        return new String(header.value(), UTF_8);
    }

    private <T> T await(KafkaFuture<T> answer) {
        try {
            return answer.get(TIMEOUT_SECONDS, SECONDS);
        } catch (ExecutionException e) {
            throw new IllegalStateException("the cluster refused: " + e.getCause().getMessage(), e);
        } catch (TimeoutException e) {
            throw new IllegalStateException(
                    "the cluster did not answer within " + TIMEOUT_SECONDS + " s", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted while asking the cluster", e);
        }
    }
}
