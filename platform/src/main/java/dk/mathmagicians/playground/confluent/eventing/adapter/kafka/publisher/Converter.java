package dk.mathmagicians.playground.confluent.eventing.adapter.kafka.publisher;

import static dk.mathmagicians.playground.confluent.eventing.adapter.kafka.EnvelopeHeaders.ID;
import static dk.mathmagicians.playground.confluent.eventing.adapter.kafka.EnvelopeHeaders.REGION;
import static dk.mathmagicians.playground.confluent.eventing.adapter.kafka.EnvelopeHeaders.SOURCE;
import static dk.mathmagicians.playground.confluent.eventing.adapter.kafka.EnvelopeHeaders.SPECVERSION;
import static dk.mathmagicians.playground.confluent.eventing.adapter.kafka.EnvelopeHeaders.SPECVERSION_VALUE;
import static dk.mathmagicians.playground.confluent.eventing.adapter.kafka.EnvelopeHeaders.TIME;
import static dk.mathmagicians.playground.confluent.eventing.adapter.kafka.EnvelopeHeaders.TYPE;
import static dk.mathmagicians.playground.confluent.eventing.adapter.kafka.EnvelopeHeaders.header;

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

/// The wire format outbound. The envelope's header fields become record headers; the payload becomes its generated
/// message, one exhaustive switch over the records. `Instant` travels as `google.protobuf.Timestamp`, a nested
/// record as a nested message. The consumer's `Reader` is the other direction.
public final class Converter {

    private Converter() {
    }

    /// The envelope's header fields as record headers, UTF-8, the instant in ISO-8601.
    public static List<Header> headers(Envelope envelope) {
        return List.of(
                header(ID, envelope.id()),
                header(REGION, envelope.region()),
                header(SOURCE, envelope.app()),
                header(TIME, envelope.at().toString()),
                header(SPECVERSION, SPECVERSION_VALUE),
                header(TYPE, to(envelope.payload()).getDescriptorForType().getFullName()));
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
}
