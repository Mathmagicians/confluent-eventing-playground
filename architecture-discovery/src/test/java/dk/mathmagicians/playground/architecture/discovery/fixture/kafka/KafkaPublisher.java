package dk.mathmagicians.playground.architecture.discovery.fixture.kafka;

import dk.mathmagicians.playground.architecture.discovery.fixture.application.Publisher;
import dk.mathmagicians.playground.architecture.discovery.fixture.domain.Thing;
import org.jmolecules.architecture.hexagonal.SecondaryAdapter;

@SecondaryAdapter
public final class KafkaPublisher implements Publisher {

    @Override
    public void publish(Thing thing) {
    }
}
