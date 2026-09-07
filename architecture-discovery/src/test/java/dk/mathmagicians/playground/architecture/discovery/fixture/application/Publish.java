package dk.mathmagicians.playground.architecture.discovery.fixture.application;

import dk.mathmagicians.playground.architecture.discovery.fixture.domain.Thing;
import org.jmolecules.architecture.hexagonal.PrimaryPort;

@PrimaryPort
public record Publish(Publisher publisher) {

    public void publish(Thing thing) {
        publisher.publish(thing);
    }
}
