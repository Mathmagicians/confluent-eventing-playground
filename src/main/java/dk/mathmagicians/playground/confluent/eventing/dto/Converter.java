package dk.mathmagicians.playground.confluent.eventing.dto;

import com.google.protobuf.Message;
import java.util.List;

/// The serialization boundary: one reflective mapping between a record and its generated message.
/// Record components match proto fields by name (`createdAt` to `created_at`). The plan is built once in the
/// constructor, which throws on any component or field without a partner.
public final class Converter<T extends Record, P extends Message> {

    /// How one value moves between the record and the message.
    sealed interface Kind permits Scalar, Time, Nested, Repeated, Packed {
    }

    /// A record in a type-variable component, carried as `google.protobuf.Any`. Converted by the registered
    /// converter for its class (`to`) or for the proto full name in the type URL (`from`).
    record Packed(List<Converter<?, ?>> payloads) implements Kind {
    }

    /// String, double, int, long, boolean: set and get as is.
    record Scalar() implements Kind {
    }

    /// `Instant` on the record, `google.protobuf.Timestamp` on the wire.
    record Time() implements Kind {
    }

    /// A nested record, converted by its own converter.
    record Nested(Converter<?, ?> converter) implements Kind {
    }

    /// A `List` on the record, a repeated field on the wire, elements converted by `element`.
    record Repeated(Kind element) implements Kind {
    }

    private final Class<T> type;
    private final P defaultInstance;
    private final List<Converter<?, ?>> payloads;

    public Converter(Class<T> type, P defaultInstance) {
        this(type, defaultInstance, List.of());
    }

    /// `payloads` are the converters a `Packed` component may carry, for records with an `Any` field.
    public Converter(Class<T> type, P defaultInstance, List<Converter<?, ?>> payloads) {
        this.type = type;
        this.defaultInstance = defaultInstance;
        this.payloads = List.copyOf(payloads);
    }

    public P to(T record) {
        throw new UnsupportedOperationException("not implemented");
    }

    public T from(P proto) {
        throw new UnsupportedOperationException("not implemented");
    }
}
