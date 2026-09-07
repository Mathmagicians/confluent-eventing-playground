package dk.mathmagicians.playground.architecture.discovery.fixture.application;

import dk.mathmagicians.playground.architecture.discovery.fixture.domain.Thing;
import org.jmolecules.architecture.hexagonal.SecondaryPort;

@SecondaryPort
public interface Publisher {

    void publish(Thing thing);
}
