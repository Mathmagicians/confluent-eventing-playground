package dk.mathmagicians.playground.confluent.eventing.adapter.kafka;

import static dk.mathmagicians.playground.confluent.eventing.adapter.kafka.KafkaFixtures.topics;
import static org.assertj.core.api.Assertions.assertThat;

import dk.mathmagicians.playground.confluent.eventing.domain.Offer;
import dk.mathmagicians.playground.confluent.eventing.domain.Order;
import dk.mathmagicians.playground.confluent.eventing.domain.Transaction;
import org.junit.jupiter.api.Test;

/// An import of the transaction schema is the subject of the imported payload's topic, in the same environment.
class ReferenceSubjectsTest {

    private final ReferenceSubjects subjects = new ReferenceSubjects();

    @Test
    void namesTheSubjectOfTheImportedPayloadsTopic() {
        var transactions = topics().of(Transaction.class);

        assertThat(subjects.subjectName("order.proto", transactions, false, null))
                .isEqualTo(topics().of(Order.class) + "-value");
        assertThat(subjects.subjectName("offer.proto", transactions, false, null))
                .isEqualTo(topics().of(Offer.class) + "-value");
    }

    @Test
    void keepsTheEnvironmentOfTheTopic() {
        assertThat(subjects.subjectName("order.proto", "prod.transactions", false, null))
                .isEqualTo("prod.orders-value");
    }
}
