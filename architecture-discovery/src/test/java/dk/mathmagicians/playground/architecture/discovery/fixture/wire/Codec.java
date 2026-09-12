package dk.mathmagicians.playground.architecture.discovery.fixture.wire;

import dk.mathmagicians.playground.architecture.discovery.fixture.domain.Thing;

public final class Codec {

    private Codec() {
    }

    public static String encode(Thing thing) {
        return thing.toString();
    }
}
