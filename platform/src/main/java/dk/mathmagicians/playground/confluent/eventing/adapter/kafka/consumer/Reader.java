package dk.mathmagicians.playground.confluent.eventing.adapter.kafka.consumer;

import static dk.mathmagicians.playground.confluent.eventing.adapter.kafka.EnvelopeHeaders.ID;
import static dk.mathmagicians.playground.confluent.eventing.adapter.kafka.EnvelopeHeaders.REGION;
import static dk.mathmagicians.playground.confluent.eventing.adapter.kafka.EnvelopeHeaders.SOURCE;
import static dk.mathmagicians.playground.confluent.eventing.adapter.kafka.EnvelopeHeaders.TIME;
import static dk.mathmagicians.playground.confluent.eventing.adapter.kafka.EnvelopeHeaders.text;

import com.google.protobuf.InvalidProtocolBufferException;
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
import io.confluent.kafka.serializers.protobuf.KafkaProtobufDeserializer;
import java.time.Instant;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Headers;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/// The wire format inbound. A record's value is the bytes on the wire: Confluent's magic byte, the schema id, and
/// the message. The reader owns the Protobuf deserializer, configured like the consumer, which resolves the id
/// through the registry and answers a message; the message's full name picks the record it becomes, one parser
/// per payload type. The envelope comes back from the headers. The publisher's `Converter` is the other direction.
@Component
@Profile("!local")
public final class Reader {

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

    private final KafkaProtobufDeserializer<Message> deserializer;

    Reader(KafkaProperties properties) {
        this.deserializer = new KafkaProtobufDeserializer<>();
        this.deserializer.configure(properties.buildConsumerProperties(), false);
    }

    /// The envelope a record carries: the payload from its value, the header fields from its headers.
    public Envelope read(ConsumerRecord<String, byte[]> record) {
        var message = deserializer.deserialize(record.topic(), record.headers(), record.value());
        return envelope(record.headers(), from(message));
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
}
