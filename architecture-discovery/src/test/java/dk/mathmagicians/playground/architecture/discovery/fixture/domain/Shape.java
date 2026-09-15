package dk.mathmagicians.playground.architecture.discovery.fixture.domain;

import org.jmolecules.ddd.annotation.ValueObject;

/// A sealed type: the documenter draws it as a box holding its permitted types.
@ValueObject
public sealed interface Shape permits Circle, Square {
}
