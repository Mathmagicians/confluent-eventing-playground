package dk.mathmagicians.playground.confluent.eventing.bdd;

import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;

import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaMetadata;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.rest.entities.SchemaReference;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.confluent.kafka.schemaregistry.protobuf.ProtobufSchema;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;

/// Test driver for the Schema Registry of the test environment, through Confluent's client configured like the
/// serializer: `schema.registry.url` and `basic.auth.*` of the `kafka` profile.
class SchemaRegistry {

    /// TopicNameStrategy: the value schema of a topic lives under this subject
    private static final String VALUE_SUBJECT = "-value";

    /// the registry's answer to a compatibility question about a subject that has no level of its own
    private static final int SUBJECT_LEVEL_NOT_SET = 40408;

    private static final int CACHE_CAPACITY = 16;

    /// Confluent's wire format: one magic byte, the schema id as four bytes, then the message
    private static final byte MAGIC_BYTE = 0;
    private static final int WIRE_HEADER = 1 + Integer.BYTES;

    private final SchemaRegistryClient client;

    SchemaRegistry(KafkaProperties properties) {
        var configs = properties.getProperties();
        this.client =
                new CachedSchemaRegistryClient(configs.get("schema.registry.url"), CACHE_CAPACITY, configs);
    }

    /// The topic's value subject carries the checked-in proto file as its latest Protobuf schema. The registry
    /// keeps the canonical form, so both sides are compared in it. Answers the subject, for the steps that go on
    /// asking about "that schema".
    String assertRegistered(String topic, String file) {
        var subject = topic + VALUE_SUBJECT;
        var latest = latest(subject);

        assertThat(latest.getSchemaType()).isEqualTo("PROTOBUF");
        assertThat(canonical(latest.getSchema(), latest.getReferences()))
                .as("%s as the registry keeps it under %s", file, subject)
                .isEqualTo(canonical(checkedIn(file), latest.getReferences()));
        return subject;
    }

    /// The registered schema defines the message.
    void assertDescribes(String subject, String message) {
        assertThat(latest(subject).getSchema()).contains("message " + message + " {");
    }

    /// The subject evolves under the level, its own or the registry's default.
    void assertCompatibility(String subject, String level) {
        assertThat(compatibility(subject)).isEqualTo(level);
    }

    /// The value on the wire starts with Confluent's magic byte and the id of the topic's latest schema, so a
    /// deserializer resolves it through the registry.
    void assertSerializedWithLatest(byte[] value, String topic) {
        var latest = latest(topic + VALUE_SUBJECT);

        assertThat(value).as("value on the wire").hasSizeGreaterThan(WIRE_HEADER);
        assertThat(value[0]).as("magic byte").isEqualTo(MAGIC_BYTE);
        assertThat(ByteBuffer.wrap(value, 1, Integer.BYTES).getInt()).as("schema id").isEqualTo(latest.getId());
    }

    @PreDestroy
    void close() throws IOException {
        client.close();
    }

    private SchemaMetadata latest(String subject) {
        try {
            return client.getLatestSchemaMetadata(subject);
        } catch (IOException | RestClientException e) {
            throw new IllegalStateException("no schema registered for " + subject + ": " + e.getMessage(), e);
        }
    }

    /// The schema's canonical form; the imports resolve through the references the registry holds for it.
    private String canonical(String schema, List<SchemaReference> references) {
        var resolved = references.stream().collect(toMap(SchemaReference::getName, this::registered));
        return new ProtobufSchema(schema, references, resolved, null, null).canonicalString();
    }

    private String registered(SchemaReference reference) {
        try {
            return client.getSchemaMetadata(reference.getSubject(), reference.getVersion()).getSchema();
        } catch (IOException | RestClientException e) {
            throw new IllegalStateException("no schema for the reference " + reference.getName(), e);
        }
    }

    private String compatibility(String subject) {
        try {
            return client.getCompatibility(subject);
        } catch (RestClientException e) {
            if (e.getErrorCode() == SUBJECT_LEVEL_NOT_SET) {
                return compatibility(null);
            }
            throw new IllegalStateException("no compatibility level for " + subject + ": " + e.getMessage(), e);
        } catch (IOException e) {
            throw new UncheckedIOException("the registry did not answer for " + subject, e);
        }
    }

    /// The proto files travel on the classpath next to the generated code.
    private String checkedIn(String file) {
        try (var proto = getClass().getResourceAsStream("/" + file)) {
            assertThat(proto).as("%s on the classpath", file).isNotNull();
            return new String(proto.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("cannot read " + file, e);
        }
    }
}
