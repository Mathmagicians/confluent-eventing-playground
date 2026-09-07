package dk.mathmagicians.playground.architecture.discovery.fixture.cli;

import dk.mathmagicians.playground.architecture.discovery.fixture.application.Publish;
import dk.mathmagicians.playground.architecture.discovery.fixture.domain.Things;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;

@PrimaryAdapter
public record Runner(Publish publish) {

    public void run() {
        publish.publish(Things.any());
    }
}
