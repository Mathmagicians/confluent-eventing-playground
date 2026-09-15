package dk.mathmagicians.playground.confluent.eventing.adapter.kafka;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.protobuf.Message;
import dk.mathmagicians.playground.confluent.eventing.domain.Envelope;
import dk.mathmagicians.playground.confluent.eventing.domain.Payload;
import java.time.Instant;
import java.util.List;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeader;

/// The envelope's header fields travel as record headers, strings named as CloudEvents names them, binary mode.
/// The names, and the headers each way: written by the publisher, read by the consumer.
public final class EnvelopeHeaders {

    public static final String SPECVERSION = "ce_specversion";
    public static final String SPECVERSION_VALUE = "1.0";
    /// The payload's message full name, as its Protobuf descriptor names it.
    public static final String TYPE = "ce_type";
    public static final String ID = "ce_id";
    public static final String SOURCE = "ce_source";
    public static final String TIME = "ce_time";
    /// A CloudEvents extension attribute, so it carries the same prefix.
    public static final String REGION = "ce_region";

    private EnvelopeHeaders() {
    }

    public static Header header(String name, String value) {
        return new RecordHeader(name, value.getBytes(UTF_8));
    }

    /// The last value under the name, as a record with the envelope in its headers carries exactly one.
    public static String text(Headers headers, String name) {
        var header = headers.lastHeader(name);
        if (header == null) {
            throw new IllegalArgumentException("no header " + name);
        }
        return new String(header.value(), UTF_8);
    }

    /// The envelope's header fields as record headers, UTF-8, the instant in ISO-8601, the type the message's
    /// full name.
    public static List<Header> headers(Envelope envelope, Message message) {
        return List.of(
                header(ID, envelope.id()),
                header(REGION, envelope.region()),
                header(SOURCE, envelope.app()),
                header(TIME, envelope.at().toString()),
                header(SPECVERSION, SPECVERSION_VALUE),
                header(TYPE, message.getDescriptorForType().getFullName()));
    }

    /// The envelope from a record's headers and its payload.
    public static Envelope envelope(Headers headers, Payload payload) {
        return new Envelope(
                text(headers, ID),
                text(headers, REGION),
                text(headers, SOURCE),
                Instant.parse(text(headers, TIME)),
                payload);
    }
}
