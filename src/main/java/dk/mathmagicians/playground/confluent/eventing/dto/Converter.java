package dk.mathmagicians.playground.confluent.eventing.dto;

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import dk.mathmagicians.playground.confluent.eventing.domain.*;
import dk.mathmagicians.playground.eventing.Schemas;
import java.util.List;

/// The serialization boundary: each record to its generated message and back, one exhaustive switch per direction.
/// `Instant` travels as `google.protobuf.Timestamp`, a nested record as a nested message, a list as a repeated field.
/// An `Envelope` travels as `Schemas.Envelope`, its payload packed as `google.protobuf.Any`.
public final class Converter {

    /// The message types an envelope may carry, the lookup for unpacking `Any`.
    private static final List<Class<? extends Message>> PAYLOADS =
            List.of(Schemas.Product.class, Schemas.Offer.class, Schemas.Order.class, Schemas.Transaction.class);

    private Converter() {
    }

    public static Schemas.Envelope to(Envelope envelope) {
        throw new UnsupportedOperationException("not implemented");
    }

    public static Envelope from(Schemas.Envelope envelope) {
        throw new UnsupportedOperationException("not implemented");
    }

    /// The payload type from `PAYLOADS` the type URL names, unpacked, then `from(Message)`.
    private static Payload from(Any any) {
        throw new UnsupportedOperationException("not implemented");
    }

    /// `Any.unpack` with its checked exception wrapped.
    private static Message unpack(Any any, Class<? extends Message> type) {
        throw new UnsupportedOperationException("not implemented");
    }

    public static Message to(Payload payload) {
        return switch (payload) {
            case Product product -> to(product);
            case Offer offer -> to(offer);
            case Order order -> to(order);
            case Transaction transaction -> to(transaction);
        };
    }

    public static Schemas.Product to(Product product) {
        throw new UnsupportedOperationException("not implemented");
    }

    public static Schemas.Offer to(Offer offer) {
        throw new UnsupportedOperationException("not implemented");
    }

    public static Schemas.Order to(Order order) {
        throw new UnsupportedOperationException("not implemented");
    }

    public static Schemas.Transaction to(Transaction transaction) {
        throw new UnsupportedOperationException("not implemented");
    }

    public static Payload from(Message message) {
        return switch (message) {
            case Schemas.Product product -> from(product);
            case Schemas.Offer offer -> from(offer);
            case Schemas.Order order -> from(order);
            case Schemas.Transaction transaction -> from(transaction);
            default -> throw new IllegalArgumentException(
                    "no record for " + message.getDescriptorForType().getFullName());
        };
    }

    public static Product from(Schemas.Product product) {
        throw new UnsupportedOperationException("not implemented");
    }

    public static Offer from(Schemas.Offer offer) {
        throw new UnsupportedOperationException("not implemented");
    }

    public static Order from(Schemas.Order order) {
        throw new UnsupportedOperationException("not implemented");
    }

    public static Transaction from(Schemas.Transaction transaction) {
        throw new UnsupportedOperationException("not implemented");
    }
}
