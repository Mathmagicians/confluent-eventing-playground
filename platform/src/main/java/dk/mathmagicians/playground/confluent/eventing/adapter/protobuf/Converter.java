package dk.mathmagicians.playground.confluent.eventing.adapter.protobuf;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import dk.mathmagicians.playground.confluent.eventing.domain.Offer;
import dk.mathmagicians.playground.confluent.eventing.domain.Order;
import dk.mathmagicians.playground.confluent.eventing.domain.Payload;
import dk.mathmagicians.playground.confluent.eventing.domain.Product;
import dk.mathmagicians.playground.confluent.eventing.domain.Transaction;
import dk.mathmagicians.playground.eventing.OfferDTO;
import dk.mathmagicians.playground.eventing.OrderDTO;
import dk.mathmagicians.playground.eventing.ProductDTO;
import dk.mathmagicians.playground.eventing.TransactionDTO;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Supplier;

/// The wire format. Outbound, the payload becomes its generated message, one exhaustive switch over the records;
/// inbound, the message's full name picks the record it becomes, one parser per payload type. `Instant` travels
/// as `google.protobuf.Timestamp`, a nested record as a nested message. The console names a message by its
/// record, `Offer`, and fills it from text, so a builder is had by that name.
public final class Converter {

    /// From the message's bytes to the payload record of its type.
    @FunctionalInterface
    private interface Parser {
        Payload parse(byte[] bytes) throws InvalidProtocolBufferException;
    }

    private static final Map<String, Parser> PARSERS = Map.of(
            ProductDTO.Product.getDescriptor().getFullName(), bytes -> from(ProductDTO.Product.parseFrom(bytes)),
            OfferDTO.Offer.getDescriptor().getFullName(), bytes -> from(OfferDTO.Offer.parseFrom(bytes)),
            OrderDTO.Order.getDescriptor().getFullName(), bytes -> from(OrderDTO.Order.parseFrom(bytes)),
            TransactionDTO.Transaction.getDescriptor().getFullName(),
            bytes -> from(TransactionDTO.Transaction.parseFrom(bytes)));

    /// A fresh builder per payload type, by the record's simple name, sorted.
    private static final Map<String, Supplier<Message.Builder>> BUILDERS = new TreeMap<>(Map.of(
            Product.class.getSimpleName(), ProductDTO.Product::newBuilder,
            Offer.class.getSimpleName(), OfferDTO.Offer::newBuilder,
            Order.class.getSimpleName(), OrderDTO.Order::newBuilder,
            Transaction.class.getSimpleName(), TransactionDTO.Transaction::newBuilder));

    private Converter() {
    }

    public static Message to(Payload payload) {
        return switch (payload) {
            case Product product -> to(product);
            case Offer offer -> to(offer);
            case Order order -> to(order);
            case Transaction transaction -> to(transaction);
        };
    }

    public static ProductDTO.Product to(Product product) {
        return ProductDTO.Product.newBuilder()
                .setProducerId(product.producerId())
                .setProductId(product.id())
                .setProductName(product.productName())
                .setProductDescription(product.productDescription())
                .setCreatedAt(to(product.createdAt()))
                .build();
    }

    public static OfferDTO.Offer to(Offer offer) {
        return OfferDTO.Offer.newBuilder()
                .setOfferId(offer.id())
                .setProductId(offer.productId())
                .setPrice(offer.price())
                .setSellerId(offer.sellerId())
                .setCreatedAt(to(offer.createdAt()))
                .build();
    }

    public static OrderDTO.Order to(Order order) {
        return OrderDTO.Order.newBuilder()
                .setId(order.id())
                .setCustomerId(order.customerId())
                .setProductId(order.productId())
                .setCreatedAt(to(order.createdAt()))
                .build();
    }

    public static TransactionDTO.Transaction to(Transaction transaction) {
        return TransactionDTO.Transaction.newBuilder()
                .setTransactionId(transaction.id())
                .setOrderRef(to(transaction.orderRef()))
                .setOfferRef(to(transaction.offerRef()))
                .setCustomerId(transaction.customerId())
                .setSellerId(transaction.sellerId())
                .setPrice(transaction.price())
                .setCreatedAt(to(transaction.createdAt()))
                .build();
    }

    private static Timestamp to(Instant at) {
        return Timestamp.newBuilder().setSeconds(at.getEpochSecond()).setNanos(at.getNano()).build();
    }

    /// The payload record of a message, whichever class the deserializer gave it: the message's full name picks
    /// the parser, and the bytes go through it.
    public static Payload from(Message message) {
        var name = message.getDescriptorForType().getFullName();
        var parser = PARSERS.get(name);
        if (parser == null) {
            throw new IllegalArgumentException("no record for " + name);
        }
        try {
            return parser.parse(message.toByteArray());
        } catch (InvalidProtocolBufferException e) {
            throw new IllegalArgumentException("not a " + name + ": " + e.getMessage(), e);
        }
    }

    public static Product from(ProductDTO.Product product) {
        return new Product(
                product.getProducerId(),
                product.getProductId(),
                product.getProductName(),
                product.getProductDescription(),
                from(product.getCreatedAt()));
    }

    public static Offer from(OfferDTO.Offer offer) {
        return new Offer(
                offer.getOfferId(),
                offer.getProductId(),
                offer.getPrice(),
                offer.getSellerId(),
                from(offer.getCreatedAt()));
    }

    public static Order from(OrderDTO.Order order) {
        return new Order(order.getId(), order.getCustomerId(), order.getProductId(), from(order.getCreatedAt()));
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

    private static Instant from(Timestamp at) {
        return Instant.ofEpochSecond(at.getSeconds(), at.getNanos());
    }

    /// The payload types by name, sorted.
    public static Set<String> types() {
        return BUILDERS.keySet();
    }

    /// A builder of the type's message, to fill from text; a name that is no type is refused with the names.
    public static Message.Builder builder(String type) {
        var builder = BUILDERS.get(type);
        if (builder == null) {
            throw new IllegalArgumentException(type + " is no payload type, " + types() + " are");
        }
        return builder.get();
    }
}
