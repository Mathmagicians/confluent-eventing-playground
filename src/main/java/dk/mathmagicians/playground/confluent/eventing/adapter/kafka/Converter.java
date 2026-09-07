package dk.mathmagicians.playground.confluent.eventing.adapter.kafka;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import dk.mathmagicians.playground.confluent.eventing.domain.Envelope;
import dk.mathmagicians.playground.confluent.eventing.domain.Offer;
import dk.mathmagicians.playground.confluent.eventing.domain.Order;
import dk.mathmagicians.playground.confluent.eventing.domain.Payload;
import dk.mathmagicians.playground.confluent.eventing.domain.Product;
import dk.mathmagicians.playground.confluent.eventing.domain.Transaction;
import dk.mathmagicians.playground.eventing.EnvelopeDTO;
import dk.mathmagicians.playground.eventing.OfferDTO;
import dk.mathmagicians.playground.eventing.OrderDTO;
import dk.mathmagicians.playground.eventing.ProductDTO;
import dk.mathmagicians.playground.eventing.TransactionDTO;
import java.time.Instant;
import java.util.List;

/// The serialization boundary: each record to its generated message and back, one exhaustive switch per direction.
/// `Instant` travels as `google.protobuf.Timestamp`, a nested record as a nested message, a list as a repeated field.
/// An `Envelope` travels as `EnvelopeDTO.Envelope`, its payload packed as `google.protobuf.Any`.
public final class Converter {

    /// The message types an envelope may carry, the lookup for unpacking `Any`.
    private static final List<Class<? extends Message>> PAYLOADS =
            List.of(ProductDTO.Product.class, OfferDTO.Offer.class, OrderDTO.Order.class, TransactionDTO.Transaction.class);

    private Converter() {
    }

    public static EnvelopeDTO.Envelope to(Envelope envelope) {
        return EnvelopeDTO.Envelope.newBuilder()
                .setId(envelope.id())
                .setRegion(envelope.region())
                .setAppid(envelope.app())
                .setTimestamp(to(envelope.at()))
                .setPayload(Any.pack(to(envelope.payload())))
                .build();
    }

    public static Envelope from(EnvelopeDTO.Envelope envelope) {
        return new Envelope(
                envelope.getId(),
                envelope.getRegion(),
                envelope.getAppid(),
                from(envelope.getTimestamp()),
                from(envelope.getPayload()));
    }

    public static Message to(Payload payload) {
        return switch (payload) {
            case Product product -> to(product);
            case Offer offer -> to(offer);
            case Order order -> to(order);
            case Transaction transaction -> to(transaction);
        };
    }

    public static Payload from(Message message) {
        return switch (message) {
            case ProductDTO.Product product -> from(product);
            case OfferDTO.Offer offer -> from(offer);
            case OrderDTO.Order order -> from(order);
            case TransactionDTO.Transaction transaction -> from(transaction);
            default -> throw new IllegalArgumentException(
                    "no record for " + message.getDescriptorForType().getFullName());
        };
    }

    public static ProductDTO.Product to(Product product) {
        return ProductDTO.Product.newBuilder()
                .setProducerId(product.producerId())
                .setProductId(product.productId())
                .setProductName(product.productName())
                .setProductDescription(product.productDescription())
                .setCreatedAt(to(product.createdAt()))
                .build();
    }

    public static Product from(ProductDTO.Product product) {
        return new Product(
                product.getProducerId(),
                product.getProductId(),
                product.getProductName(),
                product.getProductDescription(),
                from(product.getCreatedAt()));
    }

    public static OfferDTO.Offer to(Offer offer) {
        return OfferDTO.Offer.newBuilder()
                .setOfferId(offer.offerId())
                .setProductId(offer.productId())
                .setPrice(offer.price())
                .setSellerId(offer.sellerId())
                .setCreatedAt(to(offer.createdAt()))
                .build();
    }

    public static Offer from(OfferDTO.Offer offer) {
        return new Offer(
                offer.getOfferId(),
                offer.getProductId(),
                offer.getPrice(),
                offer.getSellerId(),
                from(offer.getCreatedAt()));
    }

    public static OrderDTO.Order to(Order order) {
        return OrderDTO.Order.newBuilder()
                .setId(order.id())
                .setCustomerId(order.customerId())
                .setProductId(order.productId())
                .setCreatedAt(to(order.createdAt()))
                .build();
    }

    public static Order from(OrderDTO.Order order) {
        return new Order(order.getId(), order.getCustomerId(), order.getProductId(), from(order.getCreatedAt()));
    }

    public static TransactionDTO.Transaction to(Transaction transaction) {
        return TransactionDTO.Transaction.newBuilder()
                .setTransactionId(transaction.transactionId())
                .setOrderRef(to(transaction.orderRef()))
                .setOfferRef(to(transaction.offerRef()))
                .setCustomerId(transaction.customerId())
                .setSellerId(transaction.sellerId())
                .setPrice(transaction.price())
                .setCreatedAt(to(transaction.createdAt()))
                .build();
    }

    public static Transaction from(TransactionDTO.Transaction transaction) {
        return new Transaction(
                transaction.getTransactionId(),
                from(transaction.getOrderRef()),
                from(transaction.getOfferRef()),
                transaction.getCustomerId(),
                transaction.getSellerId(),
                transaction.getPrice(),
                from(transaction.getCreatedAt()));
    }

    private static Timestamp to(Instant at) {
        return Timestamp.newBuilder().setSeconds(at.getEpochSecond()).setNanos(at.getNano()).build();
    }

    private static Instant from(Timestamp at) {
        return Instant.ofEpochSecond(at.getSeconds(), at.getNanos());
    }

    /// The payload type from `PAYLOADS` the type URL names, unpacked, then `from(Message)`.
    private static Payload from(Any any) {
        var type = PAYLOADS.stream()
                .filter(any::is)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("no payload for " + any.getTypeUrl()));
        return from(unpack(any, type));
    }

    /// `Any.unpack` with its checked exception wrapped.
    private static Message unpack(Any any, Class<? extends Message> type) {
        try {
            return any.unpack(type);
        } catch (InvalidProtocolBufferException e) {
            throw new IllegalArgumentException("payload does not parse as " + type.getSimpleName(), e);
        }
    }
}
