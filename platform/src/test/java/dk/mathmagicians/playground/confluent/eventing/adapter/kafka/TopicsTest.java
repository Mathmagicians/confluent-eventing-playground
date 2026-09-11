package dk.mathmagicians.playground.confluent.eventing.adapter.kafka;

import static dk.mathmagicians.playground.confluent.eventing.adapter.kafka.KafkaFixtures.topics;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.payloads;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dk.mathmagicians.playground.confluent.eventing.domain.Order;
import org.junit.jupiter.api.Test;

class TopicsTest {

    @Test
    void selectsTheTopicOfThePayloadType() {
        var topics = topics();

        var selected = payloads().stream().map(topics::select).toList();

        assertThat(selected).containsExactly("test.products", "test.offers", "test.orders", "test.transactions");
    }

    @Test
    void answersTheTopicOfAPayloadType() {
        assertThat(topics().of(Order.class)).isEqualTo("test.orders");
    }

    @Test
    void rejectsABlankName() {
        assertThatThrownBy(() -> new Topics("test.products", " ", "test.orders", "test.transactions"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("topics.offers");
    }
}
