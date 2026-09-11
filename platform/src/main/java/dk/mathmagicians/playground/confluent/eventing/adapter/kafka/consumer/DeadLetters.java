package dk.mathmagicians.playground.confluent.eventing.adapter.kafka.consumer;

import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/// Where a record goes when the story throws: the dead-letter topic, `<topic>.DLT`, the same partition, Spring's
/// default. The listener container factory picks the handler up by its type.
@Configuration
@Profile("!local")
class DeadLetters {

    /// No retry: what a story throws is a decision, not a hiccup.
    private static final FixedBackOff STRAIGHT_AWAY = new FixedBackOff(0L, 0L);

    /// Auto-configuration cannot express it: the record is set aside as the bytes it arrived as, so the recoverer
    /// publishes through a producer of bytes, not the Protobuf producer the publisher uses.
    @Bean
    CommonErrorHandler setAside(KafkaProperties properties) {
        var bytes = new DefaultKafkaProducerFactory<>(
                properties.buildProducerProperties(), new StringSerializer(), new ByteArraySerializer());
        return new DefaultErrorHandler(new DeadLetterPublishingRecoverer(new KafkaTemplate<>(bytes)), STRAIGHT_AWAY);
    }
}
