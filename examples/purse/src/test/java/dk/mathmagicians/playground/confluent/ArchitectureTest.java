package dk.mathmagicians.playground.confluent;

import com.tngtech.archunit.junit.AnalyzeClasses;
import dk.mathmagicians.playground.confluent.architecture.HexagonalArchitecture;
import dk.mathmagicians.playground.confluent.architecture.Production;

/// Analyze the project in the context of hexagonal architecture, verify it is compliant to the rules, as a system consisting of the platform and this application
@AnalyzeClasses(packagesOf = EventingPlatform.class, importOptions = Production.class)
class ArchitectureTest extends HexagonalArchitecture {}
