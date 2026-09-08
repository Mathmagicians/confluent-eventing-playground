package dk.mathmagicians.playground.architecture.discovery.fixture.domain;

import org.jmolecules.ddd.annotation.ValueObject;

@ValueObject
public record Circle(int radius) implements Shape {
}
