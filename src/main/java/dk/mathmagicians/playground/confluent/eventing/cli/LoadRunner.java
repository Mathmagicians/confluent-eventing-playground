package dk.mathmagicians.playground.confluent.eventing.cli;

import dk.mathmagicians.playground.confluent.eventing.domain.Offer;
import dk.mathmagicians.playground.confluent.eventing.domain.Order;
import dk.mathmagicians.playground.confluent.eventing.domain.Product;
import dk.mathmagicians.playground.confluent.eventing.load.Generator;
import dk.mathmagicians.playground.confluent.eventing.load.LoadProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

/// Inbound adapter: the command line, bound to `LoadProperties`, picks the recipe and runs a generator with it.
@Component
public final class LoadRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(LoadRunner.class);

    private final LoadProperties properties;

    public LoadRunner(LoadProperties properties) {
        this.properties = properties;
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

    /// The sink logs each event at DEBUG until the Kafka adapter takes its place. The clock is wired here, at the
    /// edge.
    private <T> long start(Generator.Recipe<T> recipe) {
        Consumer<T> sink = payload -> log.debug("Produced {} for {}", payload, properties.region());
        return new Generator<>(recipe, sink, java.time.Clock.systemUTC())
                .start(properties.concurrent(), properties.interval(), properties.ttl());
    }
}
