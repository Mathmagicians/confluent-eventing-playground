package dk.mathmagicians.playground.confluent.eventing.adapter.kafka.consumer;

import static dk.mathmagicians.playground.confluent.eventing.adapter.kafka.KafkaFixtures.topics;
import static org.assertj.core.api.Assertions.assertThat;

import dk.mathmagicians.playground.confluent.eventing.domain.Order;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;

class DeadLettersTest {

    @Test
    void handsRecordsToTheDeadLetterTopicWithoutRetry() {
        var handler = new DeadLetters().setAside(new KafkaProperties());

        assertThat(handler).isInstanceOf(DefaultErrorHandler.class);
    }

    /// The dead-letter topic is the source's name with the suffix `iac/` uses, on the same partition.
    @Test
    void setsARecordAsideOnItsTopicsDeadLetterTopicAndPartition() {
        var orders = topics().of(Order.class);
        var record = new ConsumerRecord<>(orders, 3, 42L, "key", new byte[0]);

        var destination = DeadLetters.DESTINATION.apply(record, new IllegalStateException("refused"));

        assertThat(destination).isEqualTo(new TopicPartition(orders + ".DLT", 3));
    }
}
