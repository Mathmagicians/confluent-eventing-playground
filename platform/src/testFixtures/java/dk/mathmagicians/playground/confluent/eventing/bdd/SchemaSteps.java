package dk.mathmagicians.playground.confluent.eventing.bdd;

import io.cucumber.java.en.Then;

/// Steps about the schema of a topic. Glue scope is one instance per scenario, so the subject under test lives
/// here from the step that registers it to the steps that say "that schema".
public class SchemaSteps {

    private final SchemaRegistry registry;
    private String subject;

    public SchemaSteps(SchemaRegistry registry) {
        this.registry = registry;
    }

    @Then("the Protobuf schema {word} is registered for the {topic}")
    public void theProtobufSchemaIsRegisteredForTheTopic(String file, String topic) {
        subject = registry.assertRegistered(topic, file);
    }

    @Then("that schema describes the message {word}")
    public void thatSchemaDescribesTheMessage(String message) {
        registry.assertDescribes(subject, message);
    }

    @Then("the schema evolves with {word} compatibility")
    public void theSchemaEvolvesWithCompatibility(String level) {
        registry.assertCompatibility(subject, level);
    }
}
