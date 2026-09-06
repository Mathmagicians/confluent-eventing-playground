package dk.mathmagicians.playground.confluent.eventing.kafka;

import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.envelope;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.offer;
import static dk.mathmagicians.playground.confluent.eventing.kafka.KafkaFixtures.topics;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dk.mathmagicians.playground.confluent.eventing.dto.Converter;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

@ExtendWith(MockitoExtension.class)
class KafkaPublisherTest {

    @Mock
    private KafkaTemplate<String, byte[]> template;

    @Test
    void sendsTheEnvelopeAsBytesToThePayloadsTopicUnderItsKey() {
        var envelope = envelope(offer());
        when(template.send(anyString(), anyString(), any())).thenReturn(CompletableFuture.completedFuture(null));
        var publisher = new KafkaPublisher(template, topics());

        publisher.publish(envelope);

        verify(template).send("test.offers", envelope.key(), Converter.to(envelope).toByteArray());
    }
}
