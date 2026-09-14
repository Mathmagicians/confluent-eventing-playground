package dk.mathmagicians.playground.confluent.stories.teaparty.bdd;

import dk.mathmagicians.playground.confluent.eventing.bdd.Cluster;
import dk.mathmagicians.playground.confluent.eventing.bdd.GeneratorContainer;
import dk.mathmagicians.playground.confluent.eventing.bdd.Published;
import dk.mathmagicians.playground.confluent.eventing.bdd.StoryContainer;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.util.List;

/// Steps of "At the tea party an order meets an offer", glue to `TeaPartyTable`. "a generator in region
/// <Region> has placed offers and orders" is the platform's, shared.
public class TeaPartySteps {

    private final TeaPartyTable table;

    public TeaPartySteps(StoryContainer story, Cluster cluster, GeneratorContainer generator, Published published) {
        this.table = new TeaPartyTable(story, cluster, generator, published);
    }

    @Given("a tea party in region {word}")
    public void aTeaPartyInRegion(String region) {
        table.sit(region);
    }

    @Given("a generator in region {word} has placed orders and no offers")
    public void aGeneratorInRegionHasPlacedOrdersAndNoOffers(String region) {
        table.placed(List.of("order"), region);
    }

    @Given("a generator in region {word} has placed offers and no orders")
    public void aGeneratorInRegionHasPlacedOffersAndNoOrders(String region) {
        table.placed(List.of("offer"), region);
    }

    @When("the tea party reads the streams of offers and orders")
    public void theTeaPartyReadsTheStreamsOfOffersAndOrders() {
        table.reads();
    }

    @When("the generator places offers")
    public void theGeneratorPlacesOffers() {
        table.placedMore(List.of("offer"));
    }

    @When("the generator places orders")
    public void theGeneratorPlacesOrders() {
        table.placedMore(List.of("order"));
    }

    @Then("every order for a thing on offer was settled by one transaction")
    public void everyOrderForAThingOnOfferWasSettledByOneTransaction() {
        table.assertEveryOrderOnOfferSettledOnce();
    }

    @Then("each transaction names the order's customer, the offer's seller, and the offer's price")
    public void eachTransactionNamesTheOrdersCustomerTheOffersSellerAndTheOffersPrice() {
        table.assertTransactionsNameTheParties();
    }

    @Then("each offer was taken once, by the order that waited longest")
    public void eachOfferWasTakenOnceByTheOrderThatWaitedLongest() {
        table.assertEachOfferTakenOnceByTheLongestWaiting();
    }

    @Then("nothing was settled")
    public void nothingWasSettled() {
        table.assertNothingSettled();
    }

    @Then("every order for a thing now on offer was settled")
    public void everyOrderForAThingNowOnOfferWasSettled() {
        table.reads();
        table.assertEveryOrderOnOfferSettledOnce();
    }

    @Then("every offer for a thing now ordered was taken")
    public void everyOfferForAThingNowOrderedWasTaken() {
        table.reads();
        table.assertEachOfferTakenOnceByTheLongestWaiting();
    }
}
