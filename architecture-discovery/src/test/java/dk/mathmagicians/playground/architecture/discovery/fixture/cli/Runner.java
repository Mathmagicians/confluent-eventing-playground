package dk.mathmagicians.playground.architecture.discovery.fixture.cli;

import dk.mathmagicians.playground.architecture.discovery.fixture.application.Defaults;
import dk.mathmagicians.playground.architecture.discovery.fixture.application.Publish;
import dk.mathmagicians.playground.architecture.discovery.fixture.domain.Things;
import dk.mathmagicians.playground.architecture.discovery.fixture.wire.Codec;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;

@PrimaryAdapter
public record Runner(Publish publish, Defaults defaults) {

    public void run() {
        var thing = Things.any();
        System.out.println(defaults.region() + " " + Codec.encode(thing));
        publish.publish(thing);
    }
}
