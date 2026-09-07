package dk.mathmagicians.playground.confluent.eventing.adapter.kafka;

import static dk.mathmagicians.playground.confluent.eventing.adapter.kafka.KafkaFixtures.landed;
import static dk.mathmagicians.playground.confluent.eventing.adapter.kafka.KafkaFixtures.topics;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.envelope;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.offer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.protobuf.Message;
import dk.mathmagicians.playground.confluent.eventing.domain.Receipt;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

@ExtendWith(MockitoExtension.class)
class KafkaPublisherTest {

    @Mock
    private KafkaTemplate<String, Message> template;

    @Captor
    private ArgumentCaptor<ProducerRecord<String, Message>> record;

    @Test
    void sendsThePayloadMessageToItsTopicUnderTheKeyWithTheEnvelopeAsHeaders() {
        var envelope = envelope(offer());
        when(template.send(any(ProducerRecord.class))).thenReturn(landed("test.offers", 4, 2));
        var publisher = new KafkaPublisher(template, topics());

        publisher.publish(envelope);

        verify(template).send(record.capture());
        var sent = record.getValue();
        assertThat(sent.topic()).isEqualTo("test.offers");
        assertThat(sent.key()).isEqualTo(envelope.key());
        assertThat(sent.value()).isEqualTo(Converter.to(envelope.payload()));
        assertThat(Converter.envelope(sent.headers(), envelope.payload())).isEqualTo(envelope);
    }

    @Test
    void answersAReceiptWithThePartitionAndOffsetTheMessageLandedOn() {
        var envelope = envelope(offer());
        when(template.send(any(ProducerRecord.class))).thenReturn(landed("test.offers", 4, 2));
        var publisher = new KafkaPublisher(template, topics());

        var receipt = publisher.publish(envelope).join();

        assertThat(receipt).isEqualTo(new Receipt(envelope.id(), "test.offers", 4, 2));
    }
}
