package dk.mathmagicians.playground.confluent.eventing.bdd;

import io.cucumber.java.en.Given;
import java.util.List;

/// Steps of a generator that has already published, shared by every feature that needs data on the cluster
/// before it looks: "a generator in region EMEA has placed offers and orders", "the generator has placed products
/// again". What it published is in `Published`, for the steps that look for the outcome.
public class GeneratorSteps {

    private final GeneratorContainer generator;
    private final Published published;

    public GeneratorSteps(GeneratorContainer generator, Published published) {
        this.generator = generator;
        this.published = published;
    }

    @Given("a generator in region {word} has placed {payloads}")
    public void aGeneratorInRegionHasPlaced(String region, List<String> payloads) {
        published.add(region, generator.publish(payloads, region));
    }

    @Given("the generator has placed {payloads} again")
    public void theGeneratorHasPlacedAgain(List<String> payloads) {
        published.add(published.region(), generator.publish(payloads, published.region()));
    }
}
