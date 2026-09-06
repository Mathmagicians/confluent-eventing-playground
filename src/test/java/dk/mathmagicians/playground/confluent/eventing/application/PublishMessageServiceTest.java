package dk.mathmagicians.playground.confluent.eventing.application;

import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.envelope;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.publisher;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.receipt;
import static org.assertj.core.api.Assertions.assertThat;

import dk.mathmagicians.playground.confluent.eventing.domain.Envelope;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;

class PublishMessageServiceTest {

    @Test
    void handsTheEnvelopeToThePublisherAndAnswersItsReceipt() {
        var published = new ArrayList<Envelope>();
        var useCase = new PublishMessageService(publisher(published));
        var envelope = envelope();

        var receipt = useCase.publish(envelope).join();

        assertThat(published).containsExactly(envelope);
        assertThat(receipt).isEqualTo(receipt(envelope));
    }
}
