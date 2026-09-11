package dk.mathmagicians.playground.confluent.eventing.adapter.kafka;

import static java.nio.charset.StandardCharsets.UTF_8;

import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeader;

/// The envelope's header fields travel as record headers, strings named as CloudEvents names them. The names, and
/// one header each way: written by the publisher, read by the consumer.
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
}
