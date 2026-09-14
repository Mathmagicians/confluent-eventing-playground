package dk.mathmagicians.playground.confluent.eventing.adapter.kafka;

import io.confluent.kafka.schemaregistry.ParsedSchema;
import io.confluent.kafka.serializers.subject.strategy.ReferenceSubjectNameStrategy;
import java.util.Map;

/// Where the registry keeps a schema another one imports: `order.proto`, imported by `transaction.proto`, is the
/// schema of the orders topic in the same environment, `test.orders-value`, as `iac/` registers it. The Confluent
/// serializer asks this for every import of a message before it looks the message's own schema up; its default
/// asks for a subject named after the import, which nobody registers. Set as the producer's
/// `reference.subject.name.strategy`.
public final class ReferenceSubjects implements ReferenceSubjectNameStrategy {

    private static final String VALUE = "-value";

    @Override
    public void configure(Map<String, ?> configs) {
    }

    /// `order.proto` on `test.transactions` is `test.orders-value`: the topic's environment, the import's payload
    /// in the plural, the value suffix.
    @Override
    public String subjectName(String reference, String topic, boolean isKey, ParsedSchema schema) {
        var environment = topic.substring(0, topic.indexOf('.') + 1);
        var payload = reference.substring(0, reference.indexOf('.'));
        return environment + payload + "s" + VALUE;
    }
}
