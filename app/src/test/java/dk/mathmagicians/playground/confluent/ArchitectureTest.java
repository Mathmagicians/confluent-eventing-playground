package dk.mathmagicians.playground.confluent;

import com.tngtech.archunit.junit.AnalyzeClasses;
import dk.mathmagicians.playground.confluent.architecture.HexagonalArchitecture;
import dk.mathmagicians.playground.confluent.architecture.Production;

/// The hexagonal rule over the distribution: the platform and every story on the classpath.
@AnalyzeClasses(packagesOf = EventingPlatform.class, importOptions = Production.class)
class ArchitectureTest extends HexagonalArchitecture {}
