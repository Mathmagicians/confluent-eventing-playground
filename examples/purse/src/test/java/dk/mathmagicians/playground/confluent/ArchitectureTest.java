package dk.mathmagicians.playground.confluent;

import com.tngtech.archunit.junit.AnalyzeClasses;
import dk.mathmagicians.playground.confluent.architecture.HexagonalArchitecture;
import dk.mathmagicians.playground.confluent.architecture.Production;

/// The hexagonal rule over this distribution: the platform and the purse.
@AnalyzeClasses(packagesOf = EventingPlatform.class, importOptions = Production.class)
class ArchitectureTest extends HexagonalArchitecture {}
