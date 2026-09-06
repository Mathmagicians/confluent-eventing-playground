package dk.mathmagicians.playground.confluent;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.jmolecules.archunit.JMoleculesArchitectureRules;

/// The hexagon as a checked fact. Types carry the jMolecules stereotypes, application, port, adapter, and the rule
/// fails the build when the core reaches an adapter or an adapter bypasses its port.
@AnalyzeClasses(packagesOf = ConfluentLoadGeneratorApplication.class, importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule hexagonal = JMoleculesArchitectureRules.ensureHexagonal();
}
