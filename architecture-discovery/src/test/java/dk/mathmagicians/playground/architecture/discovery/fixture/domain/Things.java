package dk.mathmagicians.playground.architecture.discovery.fixture.domain;

import org.jmolecules.ddd.annotation.Factory;

@Factory
public final class Things {

    public static Thing any() {
        return new Thing("thing");
    }

    private Things() {
    }
}
