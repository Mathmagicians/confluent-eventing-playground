package dk.mathmagicians.playground.confluent.eventing.bdd;

import dk.mathmagicians.playground.confluent.eventing.domain.Receipt;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/// Steps of "I can publish messages". Glue scope is one instance per scenario, so the generator's region and its
/// receipt live here from the Given to the last Then.
public class PublishSteps {

    private final GeneratorContainer generator;
    private final Transport transport;

    private String region;
    private Receipt receipt;

    public PublishSteps(GeneratorContainer generator, Transport transport) {
        this.generator = generator;
        this.transport = transport;
    }

    @Given("I am a generator in region {word}")
    public void iAmAGeneratorInRegion(String region) {
        this.region = region;
    }

    @When("I publish one {word} in an envelope")
    public void iPublishOneInAnEnvelope(String payload) {
        receipt = generator.publishOne(payload, region);
    }

    @Then("I receive the envelope id, and the partition and offset the message landed on")
    public void iReceiveTheEnvelopeIdAndThePartitionAndOffsetTheMessageLandedOn() {
        generator.assertReceived(receipt);
    }

    @Then("the message at that offset in the {topic} is my envelope")
    public void theMessageAtThatOffsetInTheTopicIsMyEnvelope(String topic) {
        transport.assertEnvelopeAt(topic, receipt);
    }

    @Then("the message is serialized with the protobuf schema registered for the {topic}")
    public void theMessageIsSerializedWithTheProtobufSchemaRegisteredForTheTopic(String topic) {
        transport.assertSerializedWithTheSchemaOf(topic, receipt);
    }

    @Then("the envelope names region {word}")
    public void theEnvelopeNamesRegion(String region) {
        transport.assertRegion(receipt, region);
    }
}
