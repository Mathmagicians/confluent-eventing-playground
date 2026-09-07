package dk.mathmagicians.playground.confluent.eventing.adapter.cli;

import dk.mathmagicians.playground.confluent.eventing.application.GenerateLoad;
import dk.mathmagicians.playground.confluent.eventing.domain.Offer;
import dk.mathmagicians.playground.confluent.eventing.domain.Order;
import dk.mathmagicians.playground.confluent.eventing.domain.Payload;
import dk.mathmagicians.playground.confluent.eventing.domain.Product;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/// Driving adapter: the command line, bound to `LoadProperties`, picks the payload recipe for the run and hands it
/// to the use case. A record: Spring injects the canonical constructor.
@PrimaryAdapter
@Component
public record LoadRunner(LoadProperties properties, GenerateLoad generateLoad) implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(LoadRunner.class);

    @Override
    public void run(ApplicationArguments args) {
        long produced = switch (properties.type()) {
            case PRODUCT -> start(Product::random);
            case OFFER -> start(Offer::random);
            case ORDER -> start(Order::random);
        };
        log.info("Produced {} {} events for {}", produced, properties.type(), properties.region());
    }

    private long start(GenerateLoad.Recipe<? extends Payload> payloads) {
        return generateLoad.run(payloads, properties.concurrent(), properties.interval(), properties.ttl());
    }
}
