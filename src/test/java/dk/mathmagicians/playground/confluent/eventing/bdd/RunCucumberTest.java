package dk.mathmagicians.playground.confluent.eventing.bdd;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

/// Runs every feature under `src/test/resources/features` through the JUnit Platform Suite engine: `make bdd`.
/// Step definitions live in this package. Plugins and report paths are in `junit-platform.properties`.
@Suite
@IncludeEngines("cucumber")
@SelectPackages("features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "dk.mathmagicians.playground.confluent.eventing.bdd")
class RunCucumberTest {}
