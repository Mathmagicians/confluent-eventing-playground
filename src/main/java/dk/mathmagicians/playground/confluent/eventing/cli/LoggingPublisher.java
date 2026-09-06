package dk.mathmagicians.playground.confluent.eventing.cli;

import dk.mathmagicians.playground.confluent.eventing.domain.Envelope;
import dk.mathmagicians.playground.confluent.eventing.domain.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/// Outbound adapter for the `local` profile: the log is the sink, every envelope at INFO.
@Component
@Profile("local")
final class LoggingPublisher implements Publisher {

    private static final Logger log = LoggerFactory.getLogger(LoggingPublisher.class);

    @Override
    public void publish(Envelope envelope) {
        log.info("Published {} key {}: {}", envelope.id(), envelope.key(), envelope.payload());
    }
}
