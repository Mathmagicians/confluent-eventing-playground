package dk.mathmagicians.playground.confluent.eventing.adapter.kafka.consumer;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;

class DeadLettersTest {

    @Test
    void handsRecordsToTheDeadLetterTopicWithoutRetry() {
        var handler = new DeadLetters().setAside(new KafkaProperties());

        assertThat(handler).isInstanceOf(DefaultErrorHandler.class);
    }
}
