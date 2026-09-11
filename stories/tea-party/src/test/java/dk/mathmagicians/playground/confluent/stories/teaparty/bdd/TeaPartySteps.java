package dk.mathmagicians.playground.confluent.stories.teaparty.bdd;

import io.cucumber.java.PendingException;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/// Steps of "At the tea party an order meets an offer". Every step is pending until the tea party exists.
public class TeaPartySteps {

    @Given("a tea party in region {word}")
    public void aTeaPartyInRegion(String region) {
        throw new PendingException();
    }

    // "a generator in region <Region> has placed offers and orders" is the platform's GeneratorSteps, shared

    @Given("a generator in region {word} has placed orders and no offers")
    public void aGeneratorInRegionHasPlacedOrdersAndNoOffers(String region) {
        throw new PendingException();
    }

    @When("the tea party reads the streams of offers and orders")
    public void theTeaPartyReadsTheStreamsOfOffersAndOrders() {
        throw new PendingException();
    }

    @When("the generator places offers")
    public void theGeneratorPlacesOffers() {
        throw new PendingException();
    }

    @Then("every order for a thing on offer was settled by one transaction")
    public void everyOrderForAThingOnOfferWasSettledByOneTransaction() {
        throw new PendingException();
    }

    @Then("each transaction names the order's customer, the offer's seller, and the offer's price")
    public void eachTransactionNamesTheOrdersCustomerTheOffersSellerAndTheOffersPrice() {
        throw new PendingException();
    }

    @Then("the offer is the latest one for that thing")
    public void theOfferIsTheLatestOneForThatThing() {
        throw new PendingException();
    }

    @Then("no order was settled")
    public void noOrderWasSettled() {
        throw new PendingException();
    }

    @Then("every order for a thing now on offer was settled")
    public void everyOrderForAThingNowOnOfferWasSettled() {
        throw new PendingException();
    }
}
