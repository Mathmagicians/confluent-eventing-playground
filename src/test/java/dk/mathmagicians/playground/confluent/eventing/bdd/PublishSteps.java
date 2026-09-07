package dk.mathmagicians.playground.confluent.eventing.bdd;

import dk.mathmagicians.playground.confluent.eventing.domain.Receipt;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.apache.kafka.clients.consumer.ConsumerRecord;

/// Steps of "I can publish messages". Glue scope is one instance per scenario, so the generator's region, its
/// receipt, and the record read back live here from the Given to the last Then.
public class PublishSteps {

    private final GeneratorContainer generator;
    private final Cluster cluster;
    private final SchemaRegistry registry;

    private String region;
    private Receipt receipt;
    private ConsumerRecord<String, byte[]> record;

    public PublishSteps(GeneratorContainer generator, Cluster cluster, SchemaRegistry registry) {
        this.generator = generator;
        this.cluster = cluster;
        this.registry = registry;
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
        record = cluster.assertEnvelopeAt(topic, receipt);
    }

    @Then("the message is serialized with the protobuf schema registered for the {topic}")
    public void theMessageIsSerializedWithTheProtobufSchemaRegisteredForTheTopic(String topic) {
        registry.assertSerializedWithLatest(record.value(), topic);
    }

    @Then("the envelope names region {word}")
    public void theEnvelopeNamesRegion(String region) {
        cluster.assertRegion(record, region);
    }
}
