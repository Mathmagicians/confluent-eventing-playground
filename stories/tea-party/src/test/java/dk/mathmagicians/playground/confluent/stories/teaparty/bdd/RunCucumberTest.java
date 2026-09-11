package dk.mathmagicians.playground.confluent.stories.teaparty.bdd;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

/// Runs the story's features under `src/test/resources/features` through the JUnit Platform Suite engine:
/// `make bdd`. The glue is the platform's, the cluster, the schemas, the generator, and this package's, the
/// tea party. Plugins and report paths are in the platform's `junit-platform.properties`.
@Suite
@IncludeEngines("cucumber")
@SelectPackages("features")
@ConfigurationParameter(
        key = GLUE_PROPERTY_NAME,
        value = "dk.mathmagicians.playground.confluent.eventing.bdd,"
                + "dk.mathmagicians.playground.confluent.stories.teaparty.bdd")
class RunCucumberTest {}
