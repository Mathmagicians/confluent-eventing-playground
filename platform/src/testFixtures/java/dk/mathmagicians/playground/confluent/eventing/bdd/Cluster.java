package dk.mathmagicians.playground.confluent.eventing.bdd;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.protobuf.Message;
import dk.mathmagicians.playground.confluent.eventing.adapter.kafka.EnvelopeHeaders;
import dk.mathmagicians.playground.confluent.eventing.domain.Receipt;
import io.confluent.kafka.serializers.protobuf.KafkaProtobufDeserializer;
import io.confluent.kafka.serializers.protobuf.KafkaProtobufDeserializerConfig;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;

/// Test driver for the Confluent test cluster: one admin client from the `kafka` profile for the life of the
/// suite's context, and a consumer per read, values as the raw bytes on the wire.
class Cluster {

    /// Kafka's own request timeout; the first call pays for DNS, TLS, and SASL
    private static final long TIMEOUT_SECONDS = 30;

    private final KafkaProperties properties;
    private final AdminClient client;

    Cluster(KafkaProperties properties) {
        this.properties = properties;
        this.client = AdminClient.create(properties.buildAdminProperties());
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

    /// The record at the offset, headers and raw value.
    ConsumerRecord<String, byte[]> read(String topic, int partition, long offset) {
        var configs = new HashMap<String, Object>(properties.buildConsumerProperties());
        configs.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configs.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
        configs.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        var at = new TopicPartition(topic, partition);
        try (var consumer = new KafkaConsumer<String, byte[]>(configs)) {
            consumer.assign(List.of(at));
            consumer.seek(at, offset);
            return consumer.poll(Duration.ofSeconds(TIMEOUT_SECONDS)).records(at).stream()
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("no record at " + at + " offset " + offset));
        }
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
