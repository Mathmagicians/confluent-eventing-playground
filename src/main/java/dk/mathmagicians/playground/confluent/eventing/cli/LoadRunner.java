package dk.mathmagicians.playground.confluent.eventing.cli;

import dk.mathmagicians.playground.confluent.eventing.domain.Envelope;
import dk.mathmagicians.playground.confluent.eventing.domain.Offer;
import dk.mathmagicians.playground.confluent.eventing.domain.Order;
import dk.mathmagicians.playground.confluent.eventing.domain.Payload;
import dk.mathmagicians.playground.confluent.eventing.domain.Product;
import dk.mathmagicians.playground.confluent.eventing.domain.Publisher;
import dk.mathmagicians.playground.confluent.eventing.load.Generator;
import dk.mathmagicians.playground.confluent.eventing.load.LoadProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/// Inbound adapter: the command line, bound to `LoadProperties`, picks the recipe and runs a generator with it.
@Component
public final class LoadRunner implements ApplicationRunner {

    /// The service name, stamped on every envelope. See the README Services table.
    static final String APP = "load-generator";

    private static final Logger log = LoggerFactory.getLogger(LoadRunner.class);

    private final LoadProperties properties;
    private final Publisher publisher;

    public LoadRunner(LoadProperties properties, Publisher publisher) {
        this.properties = properties;
        this.publisher = publisher;
    }

    @Override
    public void run(ApplicationArguments args) {
        long produced = switch (properties.type()) {
            case PRODUCT -> start(Product::random);
            case OFFER -> start(Offer::random);
            case ORDER -> start(Order::random);
        };
        log.info("Produced {} {} events for {}", produced, properties.type(), properties.region());
    }

    /// Wraps each payload in an envelope stamped with the region and this service, and hands it to the publisher.
    /// The clock is wired here, at the edge.
    private long start(Generator.Recipe<? extends Payload> payloads) {
        Generator.Recipe<Envelope> envelopes = (random, at) -> {
            throw new UnsupportedOperationException("LoadRunner.start");
        };
        return new Generator<>(envelopes, publisher::publish, java.time.Clock.systemUTC())
                .start(properties.concurrent(), properties.interval(), properties.ttl());
    }
}
