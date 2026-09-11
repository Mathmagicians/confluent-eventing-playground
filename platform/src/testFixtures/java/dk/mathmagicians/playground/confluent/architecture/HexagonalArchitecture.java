package dk.mathmagicians.playground.confluent.architecture;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.jmolecules.archunit.JMoleculesArchitectureRules;

/// The hexagon as a checked fact, for every distribution. Types carry the jMolecules stereotypes, port, adapter,
/// and the rule fails the build when the core reaches an adapter or an adapter bypasses its port. A distribution
/// extends this with its `@AnalyzeClasses`, the platform's root package and `Production`.
public abstract class HexagonalArchitecture {

    @ArchTest
    public static final ArchRule hexagonal = JMoleculesArchitectureRules.ensureHexagonal();
}
