package dk.mathmagicians.playground.confluent.eventing.cli;

import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.envelope;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.offer;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.order;
import static org.assertj.core.api.Assertions.assertThat;

import dk.mathmagicians.playground.confluent.eventing.domain.Receipt;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
class LoggingPublisherTest {

    @Test
    void logsTheEnvelopeIdAtInfo(CapturedOutput output) {
        var envelope = envelope();

        new LoggingPublisher().publish(envelope);

        assertThat(output.getOut()).contains("INFO").contains(envelope.id());
    }

    @Test
    void answersAReceiptCountingTheLinesAsOffsets() {
        var publisher = new LoggingPublisher();
        var first = envelope(offer());
        var second = envelope(order());

        var receipts = Stream.of(first, second).map(publisher::publish).map(CompletableFuture::join).toList();

        assertThat(receipts).containsExactly(
                new Receipt(first.id(), LoggingPublisher.TOPIC, 0, 0),
                new Receipt(second.id(), LoggingPublisher.TOPIC, 0, 1));
    }
}
