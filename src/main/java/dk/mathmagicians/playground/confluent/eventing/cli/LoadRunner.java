package dk.mathmagicians.playground.confluent.eventing.cli;

import dk.mathmagicians.playground.confluent.eventing.load.Generator;
import dk.mathmagicians.playground.confluent.eventing.load.LoadProperties;
import dk.mathmagicians.playground.confluent.eventing.load.OfferGenerator;
import dk.mathmagicians.playground.confluent.eventing.load.OrderGenerator;
import dk.mathmagicians.playground.confluent.eventing.load.ProductGenerator;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/// Inbound adapter: the command line, bound to `LoadProperties`, picks the generator and starts it.
@Component
public final class LoadRunner implements ApplicationRunner {

    private final LoadProperties properties;

    public LoadRunner(LoadProperties properties) {
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        Generator<?> generator = switch (properties.type()) {
            case PRODUCT -> new ProductGenerator();
            case OFFER -> new OfferGenerator();
            case ORDER -> new OrderGenerator();
        };
        generator.start(properties.concurrent(), properties.interval(), properties.region(), properties.ttl());
    }
}
