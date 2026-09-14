package dk.mathmagicians.playground.confluent.eventing.bdd;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import java.util.List;

/// Steps of a generator placing events in the scenario's region, shared by every feature that needs data on the
/// cluster: "a generator in region EMEA has placed offers and orders", "the generator places offers", "the
/// generator has placed products again". What it placed is in `Published`, read back in the order it landed,
/// for the steps that look for the outcome.
public class GeneratorSteps {

    private final GeneratorContainer generator;
    private final Cluster cluster;
    private final Published published;
    private final Region region;

    public GeneratorSteps(GeneratorContainer generator, Cluster cluster, Published published, Region region) {
        this.generator = generator;
        this.cluster = cluster;
        this.published = published;
        this.region = region;
    }

    @Given("a generator in region {word} has placed {payloads}")
    public void aGeneratorInRegionHasPlaced(String word, List<String> payloads) {
        place(region.of(word), payloads);
    }

    @When("the generator places {payloads}")
    public void theGeneratorPlaces(List<String> payloads) {
        place(published.region(), payloads);
    }

    @Given("the generator has placed {payloads} again")
    public void theGeneratorHasPlacedAgain(List<String> payloads) {
        place(published.region(), payloads);
    }

    private void place(String region, List<String> payloads) {
        var receipts = generator.publish(payloads, region);
        published.add(region, receipts, cluster.payloads(receipts));
    }
}
