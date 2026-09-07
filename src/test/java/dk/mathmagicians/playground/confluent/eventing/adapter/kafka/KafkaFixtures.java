package dk.mathmagicians.playground.confluent.eventing.adapter.kafka;

import java.util.concurrent.CompletableFuture;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.springframework.kafka.support.SendResult;

public final class KafkaFixtures {

    /// The test environment's topics, named as `iac/topics.tf` creates them.
    public static Topics topics() {
        return new Topics("test.products", "test.offers", "test.orders", "test.transactions");
    }

    /// The broker's answer to a send: the record landed on the partition at the offset.
    public static CompletableFuture<SendResult<String, byte[]>> landed(String topic, int partition, long offset) {
        var metadata = new RecordMetadata(new TopicPartition(topic, partition), offset, 0, 0L, 0, 0);
        return CompletableFuture.completedFuture(new SendResult<>(null, metadata));
    }

    private KafkaFixtures() {
    }
}
