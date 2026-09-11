package dk.mathmagicians.playground.confluent.eventing.application;

import static dk.mathmagicians.playground.confluent.eventing.application.StoryFixtures.publishMessage;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.APP;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.AT;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.REGION;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.dice;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.order;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.receipt;
import static org.assertj.core.api.Assertions.assertThat;

import dk.mathmagicians.playground.confluent.eventing.domain.Envelope;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class PublishMessageTest {

    private final List<Envelope> published = new ArrayList<>();
    private final PublishMessage useCase = publishMessage(published);

    @Test
    void putsThePayloadInAnEnvelopeStampedWithRegionAppAndTime() {
        var order = order();

        useCase.publish(order);

        assertThat(published).singleElement().satisfies(envelope -> {
            assertThat(envelope.region()).isEqualTo(REGION);
            assertThat(envelope.app()).isEqualTo(APP);
            assertThat(envelope.at()).isEqualTo(AT);
            assertThat(envelope.payload()).isEqualTo(order);
        });
    }

    @Test
    void drawsTheEnvelopeIdFromTheSuppliedGenerator() {
        useCase.publish(order());

        assertThat(published.getFirst().id()).isEqualTo(Envelope.id(dice()));
    }

    @Test
    void answersThePublishersReceipt() {
        var receipt = useCase.publish(order()).join();

        assertThat(receipt).isEqualTo(receipt(published.getFirst()));
    }
}
