package dk.mathmagicians.playground.confluent.eventing.adapter.protobuf;

import static java.util.stream.Collectors.joining;

import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.TextFormat;
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
import java.util.function.Supplier;

/// The wire format. Outbound, the payload becomes its generated message, one exhaustive switch over the records;
/// inbound, the message's full name picks the record it becomes, one parser per payload type. `Instant` travels
/// as `google.protobuf.Timestamp`, a nested record as a nested message. The console types a message as Protobuf
/// text for a record type, so a record is read from text by its type, and the shape of a type is had as such
/// text, for the help.
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

    /// A fresh builder of the message per record type.
    private static final Map<Class<? extends Payload>, Supplier<Message.Builder>> BUILDERS = Map.of(
            Product.class, ProductDTO.Product::newBuilder,
            Offer.class, OfferDTO.Offer::newBuilder,
            Order.class, OrderDTO.Order::newBuilder,
            Transaction.class, TransactionDTO.Transaction::newBuilder);

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

    /// The record of the type read from Protobuf text, the fields of its message as the console types them,
    /// `offer_id: "OFF-1" product_id: "P-TOPH" price: 12.5 seller_id: "MAD_HATTER"`. Text the schema refuses,
    /// a field it does not know, a value of the wrong kind, fails with the reason.
    public static <T extends Payload> T from(Class<T> type, String text) {
        var builder = builder(type);
        try {
            TextFormat.merge(text, builder);
        } catch (TextFormat.ParseException e) {
            throw new IllegalArgumentException(
                    "no " + type.getSimpleName() + " in '" + text + "': " + e.getMessage(), e);
        }
        return type.cast(from(builder.build()));
    }

    /// The shape of a type as a line the console accepts: its name, then every field with an empty value, `""`
    /// for text, `0` for a number, `false`, an enum's first value, a nested message with its own fields.
    public static String shape(Class<? extends Payload> type) {
        var descriptor = builder(type).getDescriptorForType();
        return descriptor.getName() + " " + fields(descriptor);
    }

    private static Message.Builder builder(Class<? extends Payload> type) {
        var builder = BUILDERS.get(type);
        if (builder == null) {
            throw new IllegalArgumentException("no message for " + type.getName());
        }
        return builder.get();
    }

    /// A scalar as `name: value`, a nested message as `name { fields }`, the way Protobuf prints them.
    private static String fields(Descriptor message) {
        return message.getFields().stream()
                .map(field -> field.getName() + (field.getJavaType() == FieldDescriptor.JavaType.MESSAGE ? " " : ": ")
                        + placeholder(field))
                .collect(joining(" "));
    }

    private static String placeholder(FieldDescriptor field) {
        return switch (field.getJavaType()) {
            case STRING, BYTE_STRING -> "\"\"";
            case BOOLEAN -> "false";
            case ENUM -> field.getEnumType().getValues().getFirst().getName();
            case MESSAGE -> "{ " + fields(field.getMessageType()) + " }";
            case INT, LONG, FLOAT, DOUBLE -> "0";
        };
    }
}
