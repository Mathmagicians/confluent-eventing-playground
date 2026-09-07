package dk.mathmagicians.playground.confluent.eventing.adapter.kafka;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import dk.mathmagicians.playground.confluent.eventing.domain.Envelope;
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
import java.util.List;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeader;

/// The wire format. The envelope's header fields travel as record headers, strings named as CloudEvents names them;
/// the payload travels as the record value, each record to its generated message and back, one exhaustive switch
/// per direction. `Instant` travels as `google.protobuf.Timestamp`, a nested record as a nested message.
public final class Converter {

    public static final String ID = "ce_id";
    public static final String SOURCE = "ce_source";
    public static final String TIME = "ce_time";
    /// A CloudEvents extension attribute, so it carries the same prefix.
    public static final String REGION = "ce_region";

    private Converter() {
    }

    /// The envelope's header fields as record headers, UTF-8, the instant in ISO-8601.
    public static List<Header> headers(Envelope envelope) {
        return List.of(
                header(ID, envelope.id()),
                header(REGION, envelope.region()),
                header(SOURCE, envelope.app()),
                header(TIME, envelope.at().toString()));
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

    private static Header header(String name, String value) {
        return new RecordHeader(name, value.getBytes(UTF_8));
    }

    /// The last value under the name, as a record with the envelope in its headers carries exactly one.
    private static String text(Headers headers, String name) {
        var header = headers.lastHeader(name);
        if (header == null) {
            throw new IllegalArgumentException("no header " + name);
        }
        return new String(header.value(), UTF_8);
    }
}
