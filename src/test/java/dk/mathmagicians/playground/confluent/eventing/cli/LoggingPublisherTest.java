package dk.mathmagicians.playground.confluent.eventing.cli;

import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.envelope;
import static org.assertj.core.api.Assertions.assertThat;

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
}
